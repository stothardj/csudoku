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

(defn from-square
  "Converts a set in the internal board representing a set of possibilities into
   a number, where 0 represents an empty square."
  [square]
  (if (certain? square) (first square) 0))

(defn create-board
  "Convert a list and board width,height into the internal sudoku board representation."
  [w h board]
  {:pre [(= (* (* w h) (* w h)) (count board))]}
  (let [n (* w h)]
    (into {} (map (fn [k v] [k (to-square v n)])
                  (for [r (range n) c (range n)] [r c])
                  board))))

(defn board-to-seq
  "Convert an internal board representation to a seq, exactly as passed to create board."
  [w h internal-board]
  (let [n (* w h)] (for [r (range n) c (range n)] (from-square (internal-board [r c])))))

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

(defn- choose-uncertain
  "Returns the key of an uncertain using choose strategy to select the entry to use"
  [internal-board choose-strategy]
  (->> (filter (comp uncertain? second) internal-board)
       (choose-strategy)
       (first)))

(defn first-uncertain
  "Returns the key of first uncertain spot found in a sudoku board. This is arbitrary
   as the map is unsorted. Fast so useful for solving a board. Returns nil if no such key."
  [internal-board]
  (choose-uncertain internal-board first))

(defn random-uncertain
  "Returns the key of a random uncertain spot in a sudoku board. This is really random
   and therefore useful for generating boards. Returns nil if no such key."
  [internal-board]
  (choose-uncertain internal-board #(if (empty? %) nil (rand-nth %))))

(defn board-solved?
  "Returns true if the sudoku board is solved."
  [internal-board]
  (every? certain? (vals internal-board)))

(defn board-invalid?
  "Returns true if a sudoku square in the board has no possibilities"
  [internal-board]
  (some empty? (vals internal-board)))

(defn split-board
  "Split board at a given uncertain point. Returns two boards, one with that square
   now certain and one with the remaining possibilities."
  [internal-board uncertain-key]
  {:pre [(not (board-invalid? internal-board))
         (not (board-solved? internal-board))]}
  (let [uncertain-value (internal-board uncertain-key)
        [one others] ((juxt first rest) uncertain-value)]
    [(assoc internal-board uncertain-key #{one})
     (assoc internal-board uncertain-key (into #{} others))]))

(defn print-board
  "Prints a formatted board to stdout"
  [w h internal-board]
  (let [n (* w h)]
    (letfn [(square-display [val]
              (if (certain? val)
                (first val)
                "?"))
            (print-row [r]
              (apply println (for [c (range n)]
                           (square-display (internal-board [r c])))))]
      (doseq [r (range n)]
        (print-row r)))))

(defn solve-board
  "Solves a sudoku board, returning a sequence of all possible solutions in no particular
   order."
  [w h internal-board]
  (let [keygroups (all-key-groups w h)]
    (letfn [(solve-step-single [board]
              (let [reduced (fixed #(reduce-board % keygroups reducing-strategy) board)]
                (cond
                 (board-invalid? reduced) []
                 (board-solved? reduced) [reduced]
                 :else (split-board reduced (first-uncertain reduced)))))
            (solve-step [boards]
              (let [reduced-boards (mapcat solve-step-single boards)
                    [solved unsolved] ((juxt filter remove) board-solved? reduced-boards)]
                (if (empty? unsolved)
                  solved
                  (concat solved (lazy-seq (solve-step unsolved))))))]
      (solve-step [internal-board]))))

(defn empty-board [w h]
  "Returns an empty board of given dimensions in internal board representation"
  (let [n (* w h)]
    (create-board w h (repeat (* n n) 0))))

(defn generate-solved-board [w h]
  "Returns a random solved board of given dimensions in internal board representation"
  [w h]
  (let [keygroups (all-key-groups w h)]
    (letfn [(generate-step-single [board]
              (let [reduced (fixed #(reduce-board % keygroups reducing-strategy) board)]
                (cond (board-invalid? reduced) []
                      (board-solved? reduced) [reduced]
                      :else (split-board reduced (random-uncertain reduced)))))]
      (->> (iterate #(mapcat generate-step-single %) [(empty-board w h)])
           (filter (comp board-solved? first))
           (first)
           (first)))))

(defn generate-board [w h]
  "Generates an unsolved board with open squares in internal board representation"
  [w h]
  (let [n (* w h)
        empty-square (new-square n)
        holes (shuffle (for [r (range n) c (range n)] [r c]))]
    (letfn [(still-acceptable? [board] (= 1 (count (take 2 (solve-board w h board)))))
            (punch-hole [board index]
              (let [potential (assoc board index empty-square)]
                (if (still-acceptable? potential)
                  potential
                  board)))
            (punch-holes [board indices]
              ((apply comp
                      (map #(fn [board] (punch-hole board %)) indices))
               board))]
      (punch-holes (generate-solved-board w h) holes))))
