package pl.friltase.tests

import com.nextcloud.android.sso.api.ParsedResponse
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface NextCloudService {
    @GET("{fileName}")
    fun fetchFile(@Path("fileName") fileName: String): Observable<ResponseBody>

    @GET("cloud/user?format=json")
    fun user(): Observable<OcsUser>
}