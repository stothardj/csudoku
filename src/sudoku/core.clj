(ns sudoku.core
  (:require [clojure.set :as set]))

(defn fixed
  "Apply f to a until results stop changing"
  [f a]
  (->> (iterate f a)
       (partition 2 1)
       (filter (fn [[a b]] (= a b)))
       (first)
       (first)))

;; Functions for dealing with an indivual square, represented as a set of possibilities

(defn singleton?
  "Return whether this sequence contains a single element."
  [s]
  (= 1 (count s)))

(def certain? singleton?)
(def uncertain? (comp not certain?))

(defn remove-certain
  "Given a sequence of sets, return a sequence of sets in the same order where,
   for each singleton set, the contents of that set has been removed from all
   other sets."
  [s]
  (let [certain (filter certain? s)
        uncertain (remove certain? s)
        certain-set (apply set/union certain)
        increase-certainty (fn [s] (if (certain? s) s (set/difference s certain-set)))]
    (map increase-certainty s)))

(defn only-in-one-set
  "Return a set of all the elements only contained in one of the sets."
  [& s]
  (->> (apply concat s)
       (group-by identity)
       (vals)
       (filter singleton?)
       (apply concat)
       (into #{})))

(defn certain-of-required 
  "Given a sequence of sets, return a sequence of sets in the same order where,
   for each set which is the only to contain an element, that set is now only
   contains elements unique to it."
  [s]
  (let [uniques (apply only-in-one-set s)
        to-uniques (fn [ss]
                     (let [contained-uniques (set/intersection uniques ss)]
                       (if (empty? contained-uniques) ss contained-uniques)))]
    (map to-uniques s)))

(def reducing-strategy (comp remove-certain certain-of-required))

;; A key group is a sequence of indices which have the sudoku invariants enforced on them

(defn row-key-groups
  "Generates the key groups which group rows together where n is board-size"
  [n]
  (partition n (for [r (range n) c (range n)] [r c])))

(defn col-key-groups
  "Generates the key groups which group cols together where n is board-size"
  [n]
  (partition n (for [c (range n) r (range n)] [r c])))

(defn box-key-groups
  "Generates the key groups which group boxes together where w,h is the width,height
   of a single box"
  [w h]
  (let [n (* w h)]
    (partition n (for [sr (range 0 n h)
                       sc (range 0 n w)
                       r (range sr (+ sr h))
                       c (range sc (+ sc w))]
                   [r c]))))

(defn all-key-groups
  [w h]
  (let [n (* w h)]
    (concat (row-key-groups n)
            (col-key-groups n)
            (box-key-groups w h))))

;; Generating a sudoku board internal representation

(defn new-square
  "Generates a new sudoku square with all possibilities up to and including n"
  [n]
  (into #{} (range 1 (inc n))))

(defn to-square
  "Convert a number n in the board given to a set of possibilities.
   0 represents an empty square."
  [n board-size]
  {:pre [(<= n board-size)]}
  (if (= 0 n)
    (new-square board-size)
    #{n}))

(defn create-board
  "Convert a list and board width,height into the internal sudoku board representation."
  [w h board]
  {:pre [(= (* (* w h) (* w h)) (count board))]}
  (let [n (* w h)]
    (into {} (map (fn [k v] [k (to-square v n)])
                  (for [r (range n) c (range n)] [r c])
                  board))))

;; Solving a sudoku board

(defn reduce-keygroup
  "Reduce the set of possibilities for each square referenced by a key in the keygroup
   using a given reducing strategy"
  [internal-board keygroup strategy]
  (->> (for [k keygroup] (internal-board k))
       (fixed strategy)
       (map (fn [k v] [k v]) keygroup)
       (into {})
       (merge internal-board)))

(defn reduce-board
  "Reduce the set of possibilities for each sequare referenced by all of the keygroups
   using a given reducing strategy. Goes through each keygroup sequentially."
  [internal-board keygroups strategy]
  (let [reduce-fns (map (fn [keygroup] #(reduce-keygroup % keygroup strategy)) keygroups)
        full-reduce (apply comp reduce-fns)]
    (full-reduce internal-board)))

(defn first-uncertain
  "Returns the key of first uncertain spot in a sudoku board. This is essentially arbitrary
   as the map is unsorted. Fast so useful for solving a board. Returns nil if no such key."
  [internal-board]
  (->> (filter (comp uncertain? second) internal-board)
       (first)
       (first)))

(defn split-board
  "Split board at a given uncertain point. Returns two boards, one with that square
   now certain and one with the remaining possibilities."
  [internal-board uncertain-key]
  (let [uncertain-value (internal-board uncertain-key)
        [one others] ((juxt first rest) uncertain-value)]
    [(assoc internal-board uncertain-key #{one})
     (assoc internal-board uncertain-key (into #{} others))]))

(defn board-solved?
  "Returns true if the sudoku board is solved."
  [internal-board]
  (every? certain? (vals internal-board)))
