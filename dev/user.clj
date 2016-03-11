(ns user
  (:require [reloaded.repl :refer [set-init! system init start stop go reset]]))

(set-init! #(do (require 'dev-system) ((resolve 'dev-system/dev-system))))
