def FOLDER_NAME = 'history-excerptor-job'
def JOB_NAME = 'history-excerptor'

import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*

jenkins = Jenkins.instance

def folder = jenkins.getItemByFullName(FOLDER_NAME)

// Find job in main folder
def found = jenkins.items.grep { it.name == "${JOB_NAME}" }

// Move
found.each { job ->
    Items.move(job, folder)
}

// Rename folder
folder.renameTo('history-excerptor')
