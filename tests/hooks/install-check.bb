;; Installs the package with APM into a temporary project and a temporary HOME,
;; at project scope and at user scope, and checks the deployed cljs-rules hook: the
;; launcher, the script, and the rules file are present and identical to the
;; source, the launcher is executable, and the deployed copy delivers the rules
;; at session start in a matching project.
;;
;; An install that names every target must succeed at both scopes and write
;; only for the package's targets: Claude Code and Codex get the hook, OpenCode
;; gets the skills, and no other runtime gets a file.
;;
;; Usage: bb install-check.bb <package source directory>
(require '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.string :as str])

(def here (fs/parent (fs/absolutize *file*)))
(load-file (str (fs/path here "support.bb")))
(alias 'support 'hook-test-support)

(def package-source
  (str (fs/absolutize
        (or (first *command-line-args*)
            (do (println "Usage: bb install-check.bb <package source directory>")
                (System/exit 1))))))

(def work (support/temp-directory))
(def home (str (fs/path work "home")))
(def project (str (fs/path work "proj")))
(fs/create-dirs home)
(fs/create-dirs project)
(support/git project "init" "-q")
(support/write-file project "shadow-cljs.edn" "{:builds {}}\n")

(defn apm-install [dir home-dir flags]
  (let [{:keys [exit out err]} (apply process/sh {:dir dir
                                                  :env (assoc (into {} (System/getenv)) "HOME" home-dir)
                                                  :err :string}
                                      "apm" "install" package-source flags)]
    {:exit exit :log (str out err)}))

(defn same-file? [a b]
  (and (fs/regular-file? a) (= (slurp (str a)) (slurp (str b)))))

(def hook-files ["cljs-rules.sh" "cljs-rules.bb" "cljs-rules.md"])

(doseq [[label flags] [["project" []] ["user" ["-g"]]]
        :let [{:keys [exit log]} (apm-install project home (concat flags ["--target" "claude"]))]]
  (support/check (zero? exit) (str label ": install failed (exit " exit ")"))
  (support/check (str/includes? log "hook(s) integrated") (str label ": install did not report integrated hooks"))
  (support/check (not (re-find #"(?i)hook script not found" log)) (str label ": install reported a missing hook script")))

(doseq [scope [(fs/path project ".claude") (fs/path home ".claude")]
        :let [label (str (fs/relativize work scope))
              deployed (some-> (first (fs/glob scope "**/cljs-rules.sh" {:hidden true})) fs/parent)]]
  (support/check deployed (str label ": cljs-rules.sh was not deployed"))
  (when deployed
    (doseq [file hook-files]
      (support/check (and (same-file? (fs/path deployed file) (fs/path package-source ".apm/hooks/scripts" file))
                          (or (not= file "cljs-rules.sh") (fs/executable? (fs/path deployed file))))
                     (str label ": " file " is missing, differs from the source, or is not executable")))
    (let [{:keys [exit out]} (support/run-hook (fs/path deployed "cljs-rules.sh")
                                               (json/generate-string {"session_id" (str "install-" (random-uuid))
                                                                      "hook_event_name" "SessionStart"
                                                                      "source" "startup"
                                                                      "cwd" project}))]
      (support/check (and (zero? exit)
                          (some-> (not-empty out) json/parse-string
                                  (get-in ["hookSpecificOutput" "additionalContext"])
                                  (str/includes? "CLJS-1:")))
                     (str label ": the deployed hook did not deliver the rules at session start")))))

;; The test suites are repository files, not package content.
(support/check (empty? (for [dir [(fs/path project ".claude") (fs/path home ".claude")]
                             :when (fs/exists? dir)
                             match (fs/glob dir "**tests/hooks**" {:hidden true})]
                         match))
               "the install deployed files from tests/hooks")

(defn hook-scripts
  "The script file names that a hook configuration file runs, by event. Quotes
  around project-scope commands are removed first."
  [config-file]
  (into {}
        (for [[event entries] (get (json/parse-string (slurp (str config-file))) "hooks")]
          [event (set (for [node (tree-seq coll? seq entries)
                            :when (map? node)
                            :let [command (get node "command")]
                            :when (string? command)]
                        (first (str/split (str (fs/file-name (str/replace command "\"" ""))) #"\s+"))))])))

;; The package declares Claude Code, Codex, and OpenCode as its targets, so an
;; install that names every target writes files only for those three. Claude
;; Code and Codex get the hook, and OpenCode, which has no hooks, gets the skills.
(def all-home (str (fs/path work "home-all")))
(def all-project (str (fs/path work "proj-all")))
(fs/create-dirs all-home)
(fs/create-dirs all-project)
(support/git all-project "init" "-q")

;; The events that each runtime's hook configuration must register for each
;; launcher. Codex has no PostToolUseFailure event.
(def expected-hooks
  [["Claude Code" [".claude" "settings.json"]
    {"cljs-rules.sh" ["SessionStart" "PostToolUse"]}]
   ["Codex" [".codex" "hooks.json"]
    {"cljs-rules.sh" ["SessionStart" "PostToolUse"]}]])

(def other-runtime-roots
  [".cursor" ".github" ".copilot" ".gemini" ".kiro" ".windsurf" ".codeium" ".grok"])

(defn files-under [dir]
  (when (fs/exists? dir)
    (filter fs/regular-file? (fs/glob dir "**" {:hidden true}))))

(doseq [[label flags root opencode-skills]
        [["project, all targets" ["--target" "all"] all-project (fs/path all-project ".agents" "skills")]
         ["user, all targets" ["-g" "--target" "all"] all-home (fs/path all-home ".config" "opencode" "skills")]]
        :let [{:keys [exit log]} (apm-install all-project all-home flags)
              before (:fail @support/results)]]
  (support/check (zero? exit) (str label ": install failed (exit " exit ")"))
  (doseq [[runtime config-path events-by-launcher] expected-hooks
          [launcher events] events-by-launcher
          :let [config (apply fs/path root config-path)]]
    (support/check (and (fs/exists? config)
                        (every? #(contains? (get (hook-scripts config) %) launcher) events))
                   (str label ": " runtime " does not register " launcher " for " (str/join " and " events))))
  (doseq [launcher (keys (last (second expected-hooks)))
          :let [deployed (first (filter #(= launcher (fs/file-name %)) (files-under (fs/path root ".codex"))))]]
    (support/check (and deployed (same-file? deployed (fs/path package-source ".apm/hooks/scripts" launcher)))
                   (str label ": Codex did not receive " launcher " unchanged")))
  (support/check (seq (files-under (fs/path opencode-skills "cljs-fix")))
                 (str label ": OpenCode did not receive the cljs-fix skill"))
  (support/check (empty? (mapcat #(files-under (fs/path root %)) other-runtime-roots))
                 (str label ": the install wrote files for a runtime outside the package's targets"))
  (when (> (:fail @support/results) before)
    (println (str/replace log #"(?m)^" "    "))))

(fs/delete-tree work)
(support/report "install check")
