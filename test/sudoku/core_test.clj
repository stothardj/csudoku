(ns sudoku.core-test
  (:require [clojure.test :refer :all]
            [sudoku.core :refer :all]
            [clojure.set :as set]))

(deftest fixed-test
  (is (= 10 (fixed (fn [x] (if (< x 10) (+ x 1) x)) 3))))

(deftest singleton-test
  (testing "Is a singleton"
    (are [s] (singleton? s)
         [1] '("Cake") #{3}))
  (testing "Not a singleton"
    (are [s] (not (singleton? s))
         [] [1 2 3] #{} #{1 2})))

(deftest certain-test
  (testing "Certain"
    (are [s] (certain? s)
         [1] '("Cake") #{3}))
  (testing "Uncertain"
    (are [s] (not (certain? s))
         [] [1 2 3] #{} #{1 2})))

(deftest uncertain-test
  (testing "Certain"
    (are [s] (not (uncertain? s))
         [1] '("Cake") #{3}))
  (testing "Uncertain"
    (are [s] (uncertain? s)
         [] [1 2 3] #{} #{1 2})))

(deftest remove-certain-test
  (is (= [#{1 2} #{3 4}] (remove-certain [#{1 2} #{3 4}])))
  (is (= [#{2 3} #{1}] (remove-certain [#{1 2 3} #{1}]))))

(deftest only-in-one-set-test
  (is (= #{1 2 3} (only-in-one-set #{1 2} #{3 4 5} #{4 5}))))

(deftest certain-of-required-test
  (is (= [#{1 2 3} #{2 3} #{1}] (certain-of-required [#{1 2 3} #{2 3} #{1}])))
  (is (= [#{4} #{2 3} #{1}] (certain-of-required [#{1 2 3 4} #{2 3} #{1}]))))

(deftest row-key-groups-test
  (is (= [[[0 0] [0 1] [0 2]] [[1 0] [1 1] [1 2]] [[2 0] [2 1] [2 2]]]
         (row-key-groups 3))))

(deftest col-key-groups-test
  (is (= [[[0 0] [1 0] [2 0]] [[0 1] [1 1] [2 1]] [[0 2] [1 2] [2 2]]]
         (col-key-groups 3))))

;; 2x3 board:

;; 00 01,02 03,04 05
;; 10 11,12 13,14 15
;; 20 21,22 23,24 25
;; ,,,,,,,,,,,,,,,,,
;; 30 31,32 33,34 35
;; 40 41,42 43,44 45
;; 50 51,52 53,54 55
(deftest box-key-groups-test
  (is (= [ [[0 0] [0 1] [1 0] [1 1] [2 0] [2 1]]
           [[0 2] [0 3] [1 2] [1 3] [2 2] [2 3]]
           [[0 4] [0 5] [1 4] [1 5] [2 4] [2 5]]
           [[3 0] [3 1] [4 0] [4 1] [5 0] [5 1]]
           [[3 2] [3 3] [4 2] [4 3] [5 2] [5 3]]
           [[3 4] [3 5] [4 4] [4 5] [5 4] [5 5]] ]
         (box-key-groups 2 3))))

;; 2x2 board:

;; 00 01,02 03
;; 10 11,12 13
;; ,,,,,,,,,,,
;; 20 21,22 23
;; 30 31,32 33
(deftest all-key-groups-test
  (is (= [
          ;; Rows
          [[0 0] [0 1] [0 2] [0 3]]
          [[1 0] [1 1] [1 2] [1 3]]
          [[2 0] [2 1] [2 2] [2 3]]
          [[3 0] [3 1] [3 2] [3 3]]
          ;; Cols
          [[0 0] [1 0] [2 0] [3 0]]
          [[0 1] [1 1] [2 1] [3 1]]
          [[0 2] [1 2] [2 2] [3 2]]
          [[0 3] [1 3] [2 3] [3 3]]
          ;; Boxes
          [[0 0] [0 1] [1 0] [1 1]]
          [[0 2] [0 3] [1 2] [1 3]]
          [[2 0] [2 1] [3 0] [3 1]]
          [[2 2] [2 3] [3 2] [3 3]] ]
         (all-key-groups 2 2))))

(deftest new-square-test
  (is (= #{1 2 3} (new-square 3))))

(deftest to-square-test
  (is (= #{1 2 3} (to-square 0 3)))
  (is (= #{4} (to-square 4 4))))

(deftest create-board-test
  (is (=
        (let [o #{1 2 3 4 5 6}]
          {[0 0] o [0 1] o, [0 2] o [0 3] o, [0 4] #{2} [0 5] o
           [1 0] #{1} [1 1] o, [1 2] o [1 3] #{3}, [1 4] o [1 5] o
           [2 0] o [2 1] #{2}, [2 2] o [2 3] #{5}, [2 4] o [2 5] o

           [3 0] o [3 1] o, [3 2] #{1} [3 3] o, [3 4] o [3 5] o
           [4 0] o [4 1] o, [4 2] o [4 3] o, [4 4] o [4 5] #{5}
           [5 0] #{5} [5 1] #{4}, [5 2] o [5 3] #{6}, [5 4] o [5 5] o})
         (create-board 2 3
                       [0 0, 0 0, 2 0
                        1 0, 0 3, 0 0
                        0 2, 0 5, 0 0
                        
                        0 0, 1 0, 0 0
                        0 0, 0 0, 0 5
                        5 4, 0 6, 0 0]))))

(deftest reduce-keygroup-test
  (is (= {:a 4 :b 4 :c 12 :d 9}
         (reduce-keygroup {:a 5 :b 4 :c 12 :d 9}
                          [:a :c]
                          (fn [s] (map #(if (even? %) % (dec %)) s))))))

(deftest reduce-board-test
  (is (= {:a 4 :b 4 :c 12 :d 9}
         (reduce-board {:a 5 :b 4 :c 13 :d 9}
                       [[:a] [:c]]
                       (fn [s] (map #(if (even? %) % (dec %)) s))))))

(deftest first-uncertain-test
  (is (= :c (first-uncertain {:a #{1} :b #{3} :c #{2 5}})))
  (is nil? (first-uncertain {:a #{1} :c #{3}})))

(deftest random-uncertain-test
  (is (= :c (random-uncertain {:a #{1} :b #{3} :c #{2 5}})))
  (is nil? (random-uncertain {:a #{1} :c #{3}})))

(deftest split-board-test
  (let [board {:a #{1 2 3} :b #{4 5 6} :c #{7 8}}
        [one-board other-board] (split-board board :b)]
    (is (= (dissoc board :b) (dissoc one-board :b) (dissoc other-board :b)))
    (is (certain? (:b one-board)))
    (is (= (:b board) (set/union (:b one-board) (:b other-board))))
    (is (= (count (:b board)) (+ (count (:b one-board)) (count (:b other-board)))))))

(deftest board-solved-test
  (is (board-solved? {:a #{1} :b #{2}}))
  (is (not (board-solved? {:a #{1 2} :b #{2}}))))

(deftest board-invalid-test
  (is (board-invalid? {:a #{1} :b #{}}))
  (is (not (board-invalid? {:a #{1} :b #{2 3}}))))

;; Problem (3x3 board. 0 represent open)

;; 0 0 5 0 1 2 0 4 7
;; 8 3 0 6 0 0 0 0 0
;; 0 2 1 9 0 4 3 6 5
;; 3 1 0 0 0 6 5 0 2
;; 0 5 8 1 0 0 0 0 0
;; 4 0 6 5 2 8 0 9 0
;; 1 0 2 4 9 0 7 5 0
;; 5 8 0 0 0 0 4 0 0
;; 0 4 0 8 7 0 2 1 6

;; Solution (Also verify that it correctly only reports one solution)

;; 6 9 5 3 1 2 8 4 7
;; 8 3 4 6 5 7 9 2 1
;; 7 2 1 9 8 4 3 6 5
;; 3 1 9 7 4 6 5 8 2
;; 2 5 8 1 3 9 6 7 4
;; 4 7 6 5 2 8 1 9 3
;; 1 6 2 4 9 3 7 5 8
;; 5 8 7 2 6 1 4 3 9
;; 9 4 3 8 7 5 2 1 6
(deftest solve-board-test
  (is (=
       [(create-board 3 3 [
                          6 9 5 3 1 2 8 4 7
                          8 3 4 6 5 7 9 2 1
                          7 2 1 9 8 4 3 6 5
                          3 1 9 7 4 6 5 8 2
                          2 5 8 1 3 9 6 7 4
                          4 7 6 5 2 8 1 9 3
                          1 6 2 4 9 3 7 5 8
                          5 8 7 2 6 1 4 3 9
                          9 4 3 8 7 5 2 1 6])]
       (solve-board 3 3
                    (create-board 3 3 [
                                       0 0 5 0 1 2 0 4 7
                                       8 3 0 6 0 0 0 0 0
                                       0 2 1 9 0 4 3 6 5
                                       3 1 0 0 0 6 5 0 2
                                       0 5 8 1 0 0 0 0 0
                                       4 0 6 5 2 8 0 9 0
                                       1 0 2 4 9 0 7 5 0
                                       5 8 0 0 0 0 4 0 0
                                       0 4 0 8 7 0 2 1 6])))))

(deftest empty-board-test
  (is (= (create-board 3 3 [
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            0 0 0 0 0 0 0 0 0
                            ]) (empty-board 3 3))))
