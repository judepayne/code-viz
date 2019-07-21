(ns code-viz.line_types
  (:use [clojure.java.io :only (reader)])
  (:import java.io.File))


;; Utilities
(defn blank? [^String line]
  (re-find #"^\s*$" line))

;; Line counting

;; Mason state machine
(defn mason-html-state [^String line state]
  (cond
    (re-find #"<%(perl|shared|once)>" line) (and (reset! state :perl) :perl)
    (.startsWith line "%")                                            :perl
    true                                                              :html))

(defn mason-javascript-state [^String line state]
  (cond
    (re-find #"<%(perl|shared|once)>" line) (and (reset! state :perl) :perl)
    (.startsWith line "%")                                            :perl
    true                                                              :javascript))

(defn mason-perl-state [^String line state previous-state]
  (cond
    (re-find #"</%(perl|shared|once)>" line) (and (reset! state previous-state) previous-state)
    true                                                                        :perl))

(defn mason-html-line-type [^String line state]
  (if (= @state :html)
    (mason-html-state line state)
    (mason-perl-state line state :html)))

(defn mason-javascript-line-type [^String line state]
  (if (= @state :javascript)
    (mason-javascript-state line state)
    (mason-perl-state line state :javascript)))

;; line-types Returns a seq of keywords indicating line types
;; This code was inspired by an example in "Programming Clojure" by Stuart Halloway
(defmulti line-types code-viz.files/file-type :default :text)

(defn clojure-file-parse [file clojure-dialect]
  (with-open [rdr (reader file)]
    (map (fn [^String line]
           (cond (blank? line)            :blank
                 (.startsWith line "#!/") :shebang
                 (.startsWith line ";")   :comment
                 :else                    clojure-dialect))
         (-> rdr line-seq doall))))

(defmethod line-types :clojure [file]
  (clojure-file-parse file :clojure))

(defmethod line-types :clojurescript [file]
  (clojure-file-parse file :clojurescript))

(defmethod line-types :mason-html [file]
  (let [state (atom :html)]
    (with-open [rdr (reader file)]
      (map 
       (fn [line] (mason-html-line-type line state))
       (doall (line-seq rdr))))))

(defmethod line-types :mason-javascript [file]
  (let [state (atom :javascript)]
    (with-open [rdr (reader file)]
      (map 
       (fn [line] (mason-javascript-line-type line state))
       (doall (line-seq rdr))))))

(defmethod line-types :perl [file]
  (with-open [rdr (reader file)]
    (map
     (fn [^String line]
       (cond
         (blank? line)            :blank
         (.startsWith line "#!/") :shebang
         (.startsWith line "#")   :comment
         true                     :perl))
     (doall (line-seq rdr)))))

(defmethod line-types :ruby [file]
  (with-open [rdr (reader file)]
    (map
     (fn [^String line]
       (cond
         (blank? line)            :blank
         (.startsWith line "#!/") :shebang
         (.startsWith line "#")   :comment
         true                     :ruby))
     (doall (line-seq rdr)))))

(defmethod line-types :text [file]
  (let [file-type (code-viz.files/file-type file)]
    (with-open [rdr (reader file)]
      (map
       (fn [^String line]
         (cond
           (blank? line) :blank
           true          file-type))
       (doall (line-seq rdr))))))

(defmethod line-types :xml [file]
  (with-open [rdr (reader file)]
    (map
     (fn [^String line]
       (cond
         (blank? line) :blank
         true          :xml))
     (doall (line-seq rdr)))))
