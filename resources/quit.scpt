#!/usr/bin/osascript

to appHasMenu(appName, menuBarItem, menuItem)
    tell application "System Events" to tell application process appName
        return exists (menu item menuItem of menu 1 of menu bar item menuBarItem of menu bar 1)
    end tell
end appHasMenu

on run argv
    log appHasMenu(item 1 of argv, item 2 of argv, item 3 of argv)
end run
