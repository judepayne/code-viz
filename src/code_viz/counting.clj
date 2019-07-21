(ns code-viz.counting
  (:use [code-viz.files] [code-viz.line_types])
  (:import java.io.File))


;; Utilities
(defn merge-sums [totals results]
  (merge-with + totals results))


(defn count-lines [^File file]
  (if (not (source-file? (.getName file))) {}
      (reduce
       #(merge-sums %1 {%2 1})
       {} (code-viz.line_types/line-types file))))

