(ns hornetqtest.core-test
  (:require [expectations :refer :all]
            [hornetqtest.core :refer :all]))

(def pico5 "10.146.68.45")

(def pico6 "10.146.68.46")

(def server1 {:host pico5 :port 9876})

(def server2 {:host pico6 :port 9876})

(expect #"foo" (str "boo" "foo" "ar"))
