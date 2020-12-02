package pl.friltase.tests

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.api.NextcloudAPI
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.model.SingleSignOnAccount
import com.nextcloud.android.sso.ui.UiExceptionManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.NextcloudRetrofitApiBuilder

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName

    private lateinit var api: NextcloudAPI
    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        connectAccount.setOnClickListener {
            openAccountChooser()
        }
        fetchFile.setOnClickListener {
            fetchFile()
        }
        getUser.setOnClickListener {
            getUser()
        }
        initApi()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        AccountImporter.onActivityResult(
            requestCode,
            resultCode,
            data,
            this
        ) { singleSignOnAccount ->
            SingleAccountHelper.setCurrentAccount(
                this,
                singleSignOnAccount.name
            )
            accountLabel.text = singleSignOnAccount.name
            initApi()
        }
    }

    private fun initApi() {
        try {
            val ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(this)
            accountLabel.text = ssoAccount.name
            connectApi(ssoAccount)
        } catch (e: NextcloudFilesAppAccountNotFoundException) {
            Log.e(TAG, "NextcloudFilesAppAccountNotFoundException")
        } catch (e: NoCurrentAccountSelectedException) {
            Log.e(TAG, "NoCurrentAccountSelectedException")
        }
    }

    private fun openAccountChooser() {
        try {
            AccountImporter.pickNewAccount(this)
        } catch (e: NextcloudFilesAppNotInstalledException) {
            UiExceptionManager.showDialogForException(this, e)
        } catch (e: AndroidGetAccountsPermissionNotGranted) {
            UiExceptionManager.showDialogForException(this, e)
        }
    }

    private fun connectApi(ssoAccount: SingleSignOnAccount) {
        api = NextcloudAPI(
            this,
            ssoAccount,
            GsonBuilder().create(),
            object : NextcloudAPI.ApiConnectedListener {
                override fun onConnected() {
                    Log.d(TAG, "API connected")
                }

                override fun onError(ex: Exception?) {

                }
            })
    }

    private fun fetchFile() {
        if (!this::api.isInitialized) {
            Log.e(TAG, "API not initialized")
            return
        }
        val service = NextcloudRetrofitApiBuilder(api, "/remote.php/webdav/")
            .create(NextCloudService::class.java)

        service.fetchFile("Readme.md")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Log.d(TAG, "Response: ${it.contentLength()}")
            }.also {
                disposable.add(it)
            }
    }

    private fun getUser() {
        if (!this::api.isInitialized) {
            Log.e(TAG, "API not initialized")
            return
        }
        val service = NextcloudRetrofitApiBuilder(api, "/ocs/v2.php/")
            .create(NextCloudService::class.java)

        service.user()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Log.d(TAG, "User id: ${it.ocs.data.id}")
            }.also {
                disposable.add(it)
            }
    }
}