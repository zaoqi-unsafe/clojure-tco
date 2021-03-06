## Clojure Tail Call Optimizer (CTCO)

An embedded source-to-source compiler for Clojure that provides the
benefit of full tail-call optimization (TCO) to Clojure code.

Due to Clojure's adherence to Java calling conventions, Clojure is
unable to provide full support for constant-space tail calls as is
guaranteed by languages like Scheme or Standard ML. Standard Clojure
provides some support via the `recur` form and the `trampoline`
function. The `recur` form is limited to self-recursion and using
`trampoline` requires manual code modification such that each
`trampoline`d piece of code returns a function of no arguments (or a
"thunk"). Additionally, `trampoline` doesn't allow functions to be
return values.

CTCO aims to expand support for constant-space tail calls to include
self-recursion and arbitrary n-way mutual recursion returning any
value type, including function expressions disallowed by
`trampoline`. It has been designed from the ground up to interoperate
with existing code, so it is a primary goal to allow non-CTCO-compiled
code to be able to call into CTCO-compiled code and vice versa.

CTCO works by applying a first-order one-pass CPS algorithm (via
[Danvy](http://www.cs.au.dk/~danvy/index-previous.html) 2007), then
transforming the code to return thunks, and finally creating a custom
trampoline to be used when the code is executed. Thanks to the
properties of the CPS transformation, CTCO will make all function calls
into tail calls, thereby even making non-tail code compiled by CTCO use
constant space.

**Note**: the subset of Clojure currently accepted by CTCO is very
small and will continue to grow. The grammar of the current language
is as follows:

    Expr    :=      Num  
            |       Bool  
            |       Sym  
            |       Var  
            |       String
            |       Keyword
            |       (def Var Expr)
            |       (fn [Var*] Expr*)  
            |       (fn ([Var*] Expr*)*)
            |       (defn Name [Var*] Expr*)  
            |       (defn Name ([Var*] Expr*)*)  
            |       (if Expr Expr Expr)  
            |       (cond Expr*)
            |       (let [Var Expr ...] Expr*)
            |       (Prim Expr*)
            |       (Expr Expr*)

Where:

* Num is a valid numeric type in Clojure  
* Bool is a boolean (`true` or `false`)  
* Sym is a quoted symbol  
* Var is a legal Clojure variable identifier 
* String is a Clojure string
* Keyword is a Clojure keyword
* Prim is a primitive operator/predicate in the set   
   (+ - * / mod < <= = >= > and or not inc dec zero? true? false? nil?
   instance? fn? type ref ref-set deref cons conj with-meta meta)

## Usage

The key component of CTCO is the `ctco` macro. With CTCO on your
classpath, include it with the following code:

```clojure
(use '(ctco.core :only (ctco)))
```

**Note**: This is different than previous versions (and is a breaking
change), but avoids using a single-segment namespace as before.

Then simply wrap any piece of code that you want transformed with
`(ctco ...)`. 

For example, consider the following (tail recursive) definition of
factorial:

```clojure
(defn fact
  [n a]
  (if (zero? n)
      a
      (fact (dec n) (* n a))))
```

This can be compiled to use constant stack space recursive calls by
simply wrapping it in a call to `ctco`:

```clojure
(ctco
 (defn fact
   [n a]
   (if (zero? n)
       a
       (fact (dec n) (* n a)))))
```

This will define `fact` in terms of the code transformations used by
CTCO. Simply call `fact` as you would have without the CTCO
transformations, and the rest is done for you. For reference, the
(somewhat simplified) output of the `ctco` call above generates the
following code:

```clojure
(letfn [(tramp4580 [thunk4584]
          (if (:thunk (meta thunk4584))
              (recur (thunk4584))
              thunk4584))]
  (tramp4580
   (def fact
     (fn fn4581
       ([n a]
          (tramp4580
           (with-meta
             #(fn4581 n a (with-meta (fn [x4582] x4582) {:kont true}))
             {:thunk true})))
       ([n a k4583]
          (if (zero? n)
              (k4583 a)
              (recur (dec n) (* n a) k4583)))))))
```

## Bonus: 'recurify' macro

CTCO also provides a `recurify` macro which takes any expression
accepted by the CTCO grammar and replaces all self-recursive tail
calls that explicitly use a function name to instead use the `recur`
form. It simply leverages the mechanism for doing the same
transformation within the full CTCO transformation.

For example:

```clojure
(recurify
 (defn fact
   [n a]
   (if (zero? n)
       a
       (fact (dec n) (* n a)))))
```

This will expand to the following:

```clojure
(defn fact
  [n a]
  (if (zero? n)
      a
      (recur (dec n) (* n a))))
```

## Contributing

Simply fork and use pull requests.


## Resources

A list of the resources for CTCO transformations is as follows:

* [A First-Order, One-Pass CPS Algorithm](http://www.brics.dk/RS/01/49/BRICS-RS-01-49.pdf)
  
* [Using ParentheC to Transform Scheme Programs to C or How to Write Interesting Recurive Programs in a Spartan Host (Program Counter)](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&ved=0CCUQFjAA&url=https%3A%2F%2Fwww.cs.indiana.edu%2Fcgi-pub%2Fc311%2Flib%2Fexe%2Ffetch.php%3Fmedia%3Dparenthec.pdf&ei=LNaST93BO4i46QHnyMCcBA&usg=AFQjCNG-Chb76N9lNVHO2ymtnAjo9Fvt0g&sig2=SR2itLI00reGEjRCrw-edQ&cad=rja)

## Contributors

* Alan Dipert (@alandipert)

## License

Copyright (c) 2012 Chris Frisz, Daniel P. Friedman

Distributed under the [MIT License](http://opensource.org/licenses/MIT). See accompanying LICENSE file
