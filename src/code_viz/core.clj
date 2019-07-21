(ns code-viz.core
  (:require [code-viz.counting        :as c]
            [code-viz.files           :as f]
            [clojure.java.shell       :as sh]
            [clojure.string           :as str]
            [clojure.java.io          :as io]
            [clojure.tools.cli        :refer [cli]])
  (:import java.io.File)
  (:gen-class))


;(use 'rhizome.viz)
(use 'rhizome.dot)


(defn- dir? [^File d] (.isDirectory d))


(defn- file? [^File f] (.isFile f))


(defn branch-dir?
  "Is d a branch directory (contains other directories)?
  exclusions is a seq of regexes to exclude if they match on d's name."
  ([^File d]
   (branch-dir? d nil))
  ([^File d exclusions]
   (and (dir? d)
        (every? nil? (map #(re-matches % (.getName d)) exclusions))
        (not (empty? (filter dir? (.listFiles d)))))))


(defn pretty-path
  "if p is a directory, add a leading /"
  [^File p]
  (if (dir? p)
    (str "/" (.getName p))
    (.getName p)))


(defn exclude? [^File d exclusions]
  (every? nil? (map #(re-matches % (.getName d)) exclusions)))


(defn exclude-dir?
  "pred. is the directory one of the exclusions?"
  [^File d exclusions]
  (not (every? nil? (map #(re-matches % (.getName d)) exclusions))))


(defn filter-exclusions
  "filter exclusions from the suppled seq of files and directories"
  [fs exclusions]
  (filter #(exclude? % exclusions) fs))


(defn map->seq
  "converts a map tp a string representation, one line per kv"
  [m]
  (reduce (fn [prev [k v]] (str prev (str k " " v "\n"))) "" m))


(def file-style {:shape "box" :fillcolor "cornflowerblue"
                 :style "filled" :fontsize 10})


(def folder-style {:shape "tab" :fillcolor "bisque3"
                   :style "filled" :fontcolor "white"})


(defn count-in-dir
  "count code in the specified directory"
  [^File d]
  (let [files (filter (fn [^File f] (file? f)) (.listFiles d))]
    (map->seq
     (reduce (fn [x y] (merge-with + x (c/count-lines  y))) {} files))))


(defn count-from-root [d exclusions]
  "counts code in all directories recursively down from specified directory"
  (let [files (filter (comp not dir?)
                      (tree-seq dir? 
                                (fn [^File x] (filter-exclusions (.listFiles x) exclusions))
                                d))]
    (map->seq
     (reduce (fn [x y]
               (merge-with + x (c/count-lines  y))) {} files))))


(defn label
  "return dot format label from the node (n) - which is a folder"
  [^File n exclusions]
  (cond
    (.isFile n)    (merge file-style {:label (.getName n)})
    (not (exclude? n exclusions))
    (merge folder-style {:label "*excluded*" :fillcolor "gainsboro"})
    :else
    (merge folder-style
           {:label
                                        ;(count-in-dir n)
            (count-from-root n exclusions)})))


(defn path->dot
  "convert the path (specified as a string) to a dot format"
  ([path] (path->dot path []))
  ([path exclusions]
   (tree->dot #(branch-dir? % exclusions)
              (fn [^File x] (filter (fn [^File d] (.isDirectory d)) (.listFiles x)))
              (clojure.java.io/file path)
              :node->descriptor (fn [n] (label n exclusions))
              :edge->descriptor (fn [s ^File e]
                                  {:label (if (.isDirectory e) (pretty-path e))})
              :options {:dpi 60 :fontsize 8})))


(defn- format-error [s err]
  (apply str
    err "\n"
    (interleave
      (map
        (fn [idx s]
          (format "%3d: %s" idx s))
        (range)
        (str/split-lines s))
      (repeat "\n"))))


(defn dot->svg
  [dot graphviz-path]
  (let [s' (str/replace dot "\\\\n" "\n")
        {:keys [out err]} (sh/sh graphviz-path "-Tsvg" :in s')]
    (or
      out
      (throw (IllegalArgumentException. ^String (str "Graphviz!: "(format-error s' err)))))))


(defn path->svg
  "convert the path (specified as a string) to an svg representation"
  ([path filename] (path->svg path filename "dot" []))
  ([path filename graphviz-path] (path->svg path filename graphviz-path []))
  ([path filename graphviz-path exclusions]
   (spit filename
         (-> (path->dot path exclusions)
             (dot->svg graphviz-path)))))


;; -------------------------The problem in hand----------------------

(def dot-regex #"\.(.*)")
(def resources-regex #"resources")
(def a-regex #"a(.*)")
(def classes-regex #"classes")
(def target-regex #"target")
(def node-regex #"node_components")
(def bower-regex #"bower_components")

(def ^:dynamic *exclusions* [dot-regex resources-regex classes-regex target-regex
                   node-regex bower-regex])

;(path->svg "/Users/jude/Documents/clojure" "clojure.svg" *exclusions*)


(defn file->exclusions
  [^File file]
  (let [lines (str/split-lines (slurp file))]
    (mapv re-pattern lines)))


(def required-opts #{})


(defn missing-required?
  "Returns true if opts is missing any of the required-opts"
  [opts]
  (not-every? opts required-opts))


(defn -main [& args]
  (let [[opts args banner]
        (cli args
             ["-h" "--help" "Print this help"
              :default false :flag true]
             ["-p" "--path" "Source code path to scan."
              :default "." :flag false :parse-fn identity]
             ["-o" "--out" "The name of the svg file to output. e.g. 'out.svg'"
              :default "out.svg" :flag false :parse-fn identity]
             ["-g" "--graphviz" "The path to the graphviz executable on your system."
              :default "dot" :flag false :parse-fn identity]
             ["-e" "--exclude" (str "The path of file of the exclusions file. "
                                    "The exclusions files is a set of regexes (one per line) "
                                    "of folder names to exclude when a regex is matched.")
              :default "exclusions" :flag false :parse-fn identity]
             )]
    (println opts)
    (cond
      (or (:help opts) (missing-required? opts)) (println banner)
      :else (do (path->svg (:path opts)
                           (:out opts)
                           (:graphviz opts)
                           (file->exclusions (:exclude opts)))
                (System/exit 0)))))
