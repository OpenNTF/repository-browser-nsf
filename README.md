# Repository Browser NSF

This project is an NSF that provides a unified p2 repository for a collection of distinct repositories in different formats. Currently, it allows for accessing repositories from two types of locations:

- Via the filesystem in the `domino/html/repository` directory inside the Domino server's data directory. Repositories within this should be kept in their own folders, and can be within organizing subfolders
- Via Update Site NSFs using IBM's template or the [OpenNTF enhanced version](https://www.openntf.org/main.nsf/project.xsp?r=project/Open%20Eclipse%20Update%20Site). These can be specified in the Notes UI of the application by creating "Update Site NSF" documents. The names given should be unique among the named NSFs and the top-level filesystem repository folders.

These sources are concatenated into a single tree, and the app provides composite repository metadata in the root to allow Eclipse to crawl all of the available repositories.