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

(defn remove-certain
  "Given a sequence of sets, return a sequence of sets in the same order where,
   for each singleton set, the contents of that set has been removed from all
   other sets."
  [s]
  (let [certain (filter singleton? s)
        uncertain (remove singleton? s)
        certain-set (apply set/union certain)
        increase-certainty (fn [s] (if (singleton? s) s (set/difference s certain-set)))]
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
  "Convert a list and board width,height into the internal sudoku board representation"
  [w h board]
  {:pre [(= (* (* w h) (* w h)) (count board))]}
  (let [n (* w h)]
    (into {} (map (fn [k v] [k (to-square v n)])
                  (for [r (range n) c (range n)] [r c])
                  board))))
