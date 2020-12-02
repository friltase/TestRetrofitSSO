package pl.friltase.tests

data class OcsUser(
    val ocs: Ocs
) {
    data class Ocs(
        val data: UserData
    ) {
        data class UserData(
            val id: String
        )
    }
}
