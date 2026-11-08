package de.ml.backupfromchangelist.backupfromchangelist

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataConstants
import com.intellij.openapi.project.Project
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurationManager
import com.jetbrains.plugins.webDeployment.config.ServerBaseDeployable

import com.jetbrains.plugins.webDeployment.config.GroupedServersConfigManager



class ListServersAction : AnAction("List SFTP Servers") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val manager = DeploymentConfigurationManager.getInstance(project)
/*
        val servers: Collection<DeploymentServerConfiguration> = manager.servers

        servers.forEach { server ->
            if (server is SshDeploymentServerConfiguration) {
                println("SFTP Server: ${server.presentableName}")
                println("  Host: ${server.host}")
                println("  Port: ${server.port}")
                println("  User: ${server.userName}")
                // Credentials musst du über PasswordSafe holen
            }
        }
*/


            var remoteServers = GroupedServersConfigManager.getInstance(project).getFlattenedServers()

            remoteServers.forEach{server ->
                println(server)
            }



    }
}
