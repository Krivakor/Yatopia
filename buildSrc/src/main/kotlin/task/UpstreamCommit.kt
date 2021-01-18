package task

import ensureSuccess
import gitCmd
import org.gradle.api.Project
import org.gradle.api.Task
import taskGroup
import toothpick
import upstreamDir
import upstreams
import java.io.File
import Upstream

internal fun Project.createUpstreamCommitTask(
    receiver: Task.() -> Unit = {}
): Task = tasks.create("upstreamCommit") {
    receiver(this)
    group = taskGroup
    doLast {
        val changedUpstreams = ArrayList<String>()
        var gitChangelog = ""

        gitChangelog += getUpstreamChanges(project,this, changedUpstreams, gitChangelog, toothpick.upstream,
            upstreamDir, toothpick.upstream)

        for (upstream in upstreams) {
            gitChangelog += getUpstreamChanges(project,this, changedUpstreams, gitChangelog, upstream.name,
                upstream.repoPath.toFile(), "upstream/${upstream.name}")
        }

        val changedUpstreamsString = ""
        for (upstreamName in changedUpstreams) {
            if (changedUpstreams.isNotEmpty()) {
                changedUpstreams += "/"
            }
            changedUpstreams += upstreamName
        }

        if (changedUpstreamsString.isNotEmpty()) {
            val commitMessage = """
                    |Updated Upstream and Sidestream(s) ($changedUpstreamsString)
                    |
                    |Upstream/An Sidestream has released updates that appears to apply and compile correctly
                    |This update has NOT been tested by YatopiaMC and as with ANY update, please do your own testing.
                    |
                    |
                    |$gitChangelog
                """.trimMargin()
            ensureSuccess(gitCmd("commit", "-m", commitMessage, printOut = true))
        }

    }
}

private fun getUpstreamChanges(
    project: Project,
    task: Task,
    changedUpstreams: ArrayList<String>,
    gitChangelog: String,
    name: String,
    dir: File,
    path: String
): String {
    var gitChangelog1 = gitChangelog
    var oldRev = ensureSuccess(project.gitCmd("ls-tree", "HEAD", path))
        ?.substringAfter("commit ")?.substringBefore("\t")
    var changes = true;
    var upstreamTmp = ensureSuccess(
        project.gitCmd(
            "log",
            "--oneline",
            "$oldRev...HEAD",
            printOut = true,
            dir = dir
        )
    ) {
        changes = false;
        task.logger.lifecycle("No $name changes to commit.")
    }
    if (changes) {
        changedUpstreams.add("$name")
        gitChangelog1 += "$name Changes:\n$upstreamTmp\n"
    }
    return gitChangelog1
}
