(ns hornetqtest.core-test
  (:require [expectations :refer :all]
            [hornetqtest.core :refer :all]))

(def pico5 "10.146.68.45")

(def pico6 "10.146.68.46")

(expect #"foo" (str "boo" "foo" "ar"))
