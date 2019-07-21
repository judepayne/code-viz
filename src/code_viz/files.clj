(ns code-viz.files
  (:require [clojure.java.io])
  (:import java.io.File))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Java stuff

(defn filenames [^File path]
  (map
   (fn [^File x] (.toString x))
    (filter
     (fn [^File x] (.isFile x))
      (file-seq (clojure.java.io/file (.toString path))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; File selection
(defn version-control? [filename]
  (re-find #"(^|/)(\.git|\.svn)/" filename))
(defn settings? [filename]
  (re-find #"/(\.htaccess|\.gitignore|\.hgignore|.*properties)$" filename))
(defn binary? [filename]
  (re-find #"\.(doc|dot|exe|gif|jpe?g|ods|odt|ott|png|ttf|xls)$" filename))
(defn backup? [filename]
  (re-find #"/(.*\~$|.*#$)" filename))
(defn editor-related? [filename]
  (re-find #"/(\.lein-repl-history)$" filename))
(defn dependencies? [filename]
  (re-find #"/(.*dependencies$)" filename))
(defn licenses? [filename]
  (re-find #"LICENSE" filename))
(defn log-files? [filename]
  (re-find #"/(.*log$)" filename))
(defn source-file? [filename]
  (cond
    (version-control? filename) false
    (settings? filename)        false
    (binary? filename)          false
    (backup? filename)          false
    (editor-related? filename)  false
    (dependencies? filename)    false
    (licenses? filename)        false
    (log-files? filename)       false
    true                        true))

;; File type inference
(defn clojure-file-name? [^String filename] (.endsWith filename ".clj"))
(defn clojurescript-file-name? [^String filename] (.endsWith filename ".cljs"))
(defn css-file-name? [^String filename] (.endsWith filename ".css"))
(defn html-file-name? [^String filename] (re-find #"\.html?$" filename))
(defn javascript-file-name? [^String filename] (.endsWith filename ".js"))
(defn java-file-name? [^String filename] (.endsWith filename ".java"))

(defn mason-html-file-name? [^String filename]
  (or
   (re-find #"\.(mhtml|mcp)$" filename)
   (.endsWith filename "/autohandler")
   (.endsWith filename  "/dhandler")))
(defn mason-javascript-file-name? [^String filename] (re-find #"\.(mjs)$" filename))
(defn perl-file-name? [^String filename] (re-find #"\.p[lm]$" filename))
(defn ruby-file-name? [^String filename]
  (or
    (.endsWith filename ".rb")
    (.endsWith filename ".gemspec")
    (.endsWith filename "/Gemfile")
    (.endsWith filename "/Gemfile.lock")
    (.endsWith filename "/Rakefile")))
(defn sql-file-name? [^String filename] (.endsWith filename ".sql"))
(defn text-file-name? [^String filename]
  (or
    (.endsWith filename ".txt")
    (.endsWith filename "/COPYING")
    (.endsWith filename ".md")
    (re-find #"\WREADME" filename)))
(defn xml-file-name? [^String filename] (re-find #"\.(xmi|xml|xsl)$" filename))
(defn yaml-file-name? [^String filename]
  (or
    (re-find #"\.ya?ml$" filename)
    (.endsWith filename "/.rspec")))
(defn owl-file-name? [^String filename] (.endsWith filename ".owl"))
(defn groovy-file-name? [^String filename]
  (or
   (.endsWith filename ".groovy")
   (.endsWith filename ".gvy")
   (.endsWith filename ".gy")
   (.endsWith filename ".gsh")))
(defn shell-script-file-name? [^String filename]
  (or
   (.endsWith filename ".sh")
   (.endsWith filename ".ksh")
   (.endsWith filename ".csh")
   (.endsWith filename ".bsd")))
(defn xsd-file-name? [^String filename]
  (.endsWith filename ".xsd"))
(defn text-file-name? [^String filename]
  (or
   (.endsWith filename ".txt")
   (.endsWith filename ".md")
   (.endsWith filename ".markdown")
   (.endsWith filename ".adoc")))

(defn is-shebang-of-type? [line type]
  (re-find (re-pattern (str "#![a-z/]*\\s?" type)) line))

(defn is-shebang? [rdr]
  (let [first (first (line-seq rdr))]
    (or
     (and (is-shebang-of-type? first "perl") :perl)
     (and (is-shebang-of-type? first "ruby") :ruby)
     false)))

(defn file-type-from-contents [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (or
     (is-shebang? rdr)
     :unknown)))

(defn file-empty? [filename]
  (= (.length (clojure.java.io/file filename)) 0))

(defn file-type [^File file]
  (let [filename (.toString file)]
    (or
     (and (clojure-file-name? filename)          :clojure)
     (and (clojurescript-file-name? filename)    :clojurescript)
     (and (css-file-name? filename)              :css)
     (and (html-file-name? filename)             :html)
     (and (javascript-file-name? filename)       :javascript)
     (and (mason-html-file-name? filename)       :mason-html)
     (and (mason-javascript-file-name? filename) :mason-javascript)
     (and (perl-file-name? filename)             :perl)
     (and (ruby-file-name? filename)             :ruby)
     (and (sql-file-name? filename)              :sql)
     (and (text-file-name? filename)             :text)
     (and (xml-file-name? filename)              :xml)
     (and (yaml-file-name? filename)             :yaml)
     (and (java-file-name? filename)             :java)
     (and (file-empty? file)                     :empty)
     (and (owl-file-name? filename)              :owl)
     (and (groovy-file-name? filename)           :groovy)
     (and (shell-script-file-name? filename)     :shell)
     (and (xsd-file-name? filename)       :xsd)
     (and (text-file-name? filename)             :text)
     (file-type-from-contents file))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Public interface

(defn source-files [path]
  (filter
    source-file?
    (filenames path)))

(defn file-types [path]
  (map
    #(list % (file-type %))
    (source-files path)))

(defn files-of-type [path type]
  (filter
    #(= (file-type %) type)
    (source-files path)))
