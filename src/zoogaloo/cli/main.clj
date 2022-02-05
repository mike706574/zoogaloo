(ns zoogaloo.cli.main
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [zoogaloo.cli.config :as config]
            [zoogaloo.cli.core :as core]))

(def cli-options
  ;; An option with a required argument
  [["-e" "--env ENV" "Environment"
    :default "prod"]
   ["-u" "--client-url URL" "Client URL"]
   ["-p" "--password PASSWORD" "Password"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["zoogaloo CLI."
        ""
        "Usage: zoogaloo [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  todo  TODO"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn assoc-in-when [m ks v]
  (if v
    (assoc-in m ks v)
    m))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [{:keys [client-url env password]} options
            config (-> (config/config {:env env})
                       (assoc-in-when [:client :url] client-url)
                       (assoc-in-when [:client :password] password))
            deps (config/deps config)]
        (case action
          (exit 1 (str "Invalid action: " action)))))))
