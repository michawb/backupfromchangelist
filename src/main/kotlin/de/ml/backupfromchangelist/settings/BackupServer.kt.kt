package de.ml.backupfromchangelist.settings

data class BackupServer(
    var name: String = "",
    var host: String = "",
    var port: Int = 22,
    var user: String = "",
    var authMethod: String = "password",
    var password: String? = null,
    var privateKeyPath: String? = null
) {
    override fun toString() = name
}