package com.example.spamblackhole2

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.spamblackhole2.databinding.ActivityEmailBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val binding by lazy { ActivityEmailBinding.inflate(layoutInflater) }

    private lateinit var mAdapter: EmailAdapter

    private lateinit var mEmailList: ArrayList<EmailData>
    private lateinit var mSpamList : ArrayList<EmailData>
    private lateinit var mHamList : ArrayList<EmailData>

    private lateinit var credential: GoogleAccountCredential
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var spamClassifier: SpamClassifier

    private var nextPageToken: String? = null
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        drawerLayout = binding.drawerLayout

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        mEmailList = ArrayList()
        mSpamList = ArrayList()
        mHamList = ArrayList()
        mAdapter = EmailAdapter(mEmailList) { email ->
            navigateToEmailDetail(email.id)
        }
        binding.recyclerView.adapter = mAdapter

        spamClassifier = SpamClassifier(this)
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        val account = intent.getParcelableExtra<GoogleSignInAccount>("account")
        account?.let{
            fetchEmails(it)
        }

        binding.navigationView.setCheckedItem(R.id.nav_ham)
        showHamEmails()

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pasVisibleItems = layoutManager.findFirstVisibleItemPosition()

                if(!isLoading && (visibleItemCount + pasVisibleItems) >= totalItemCount){
                    isLoading = true
                    account?.let{
                        fetchEmails(it)
                    }
                }
            }
        })
    }

    private fun showHamEmails() {
        mAdapter.updateEmails(mHamList)
    }

    private fun fetchEmails(account: GoogleSignInAccount) {
        credential = GoogleAccountCredential.usingOAuth2(
            this, listOf("https://www.googleapis.com/auth/gmail.readonly")
        )
        credential.selectedAccount = android.accounts.Account(account.email, "com.google")

        val gmailService = Gmail.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("Gmail Reader")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try{
                val request = gmailService
                    .users()
                    .messages()
                    .list("me")
                    .setMaxResults(10L)
                    .setPageToken(nextPageToken)

                val messagesResponse: ListMessagesResponse = request.execute()
                nextPageToken = messagesResponse.nextPageToken
                val messages: List<Message> = messagesResponse.messages ?: emptyList()

                val newEmails = ArrayList<EmailData>()
                for(message in messages){
                    val msg = gmailService
                        .users()
                        .messages()
                        .get("me", message.id).setFormat("full")
                        .execute()

                    val headers = msg.payload.headers
                    val subjectHeader = headers.find{it.name=="Subject"}
                    val subject = subjectHeader?.value?:"No Subject"
                    val snippet = msg.snippet
                    val body = getBodyFromMessage(msg)
                    val emailData = EmailData(message.id, subject, snippet)

                    mEmailList.add(emailData)
                    newEmails.add(emailData)
                    if(spamClassifier.classify(body) == "Spam"){
                        mSpamList.add(emailData)
                    } else {
                        mHamList.add(emailData)
                    }
                }
                withContext(Dispatchers.Main){
                    mAdapter.notifyDataSetChanged()
                    isLoading = false
                }
            } catch (e:Exception){
                Log.e("EmailActivity", "Error fetching emails", e)
                withContext(Dispatchers.Main){
                    isLoading = false
                }
            }
        }
    }

    private fun getBodyFromMessage(message: Message): String {
        val parts = message.payload.parts
        var body = ""

        if (parts != null) {
            for (part in parts) {
                if (part.mimeType == "text/plain" || part.mimeType == "text/html") {
                    val data = part.body.data
                    if (data != null) {
                        body = String(Base64.decode(data, Base64.URL_SAFE))
                        break
                    }
                }
            }
        } else {
            val data = message.payload.body.data
            if (data != null) {
                body = String(Base64.decode(data, Base64.URL_SAFE))
            }
        }

        return body
    }

    private fun navigateToEmailDetail(emailId:String){
        val intent = Intent(this, EmailDetailActivity::class.java)
        intent.putExtra("email_id", emailId)
        startActivity(intent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_all_emails -> {
                mAdapter.updateEmails(mEmailList)
            }
            R.id.nav_ham -> {
                mAdapter.updateEmails(mHamList)
            }
            R.id.nav_spam -> {
                mAdapter.updateEmails(mSpamList)
            }
            R.id.nav_logout -> {
                signOut()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}