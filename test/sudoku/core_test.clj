(ns sudoku.core-test
  (:require [clojure.test :refer :all]
            [sudoku.core :refer :all]))

(deftest fixed-test
  (is (= 10 (fixed (fn [x] (if (< x 10) (+ x 1) x)) 3))))

(deftest singleton-test
  (testing "Is a singleton"
    (are [s] (singleton? s)
         [1] '("Cake") #{3}))
  (testing "Not a singleton"
    (are [s] (not (singleton? s))
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