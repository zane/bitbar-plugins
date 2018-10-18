(package-initialize)

(require 'org)
(require 'org-agenda)

(org-batch-agenda-csv "a"
                      org-agenda-span 'day
                      org-agenda-files (quote ("/Users/zane/Dropbox/org/notes.org"))
                      org-agenda-skip-deadline-if-done t
                      org-agenda-skip-deadline-prewarning-if-scheduled 'pre-scheduled
                      org-agenda-skip-scheduled-if-deadline-is-shown t)
