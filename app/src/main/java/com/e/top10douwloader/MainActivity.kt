package com.e.top10douwloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""

//    override fun toString(): String {
//        return """
//            name = $name
//            artist = $artist
//            releaseDate = $releaseDate
//            imageURL = $imageURL
//        """.trimIndent()
//    }
}

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var downloadData: DownloadData? = null

    private var feedURL: String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/sf=143462/limit=%d/xml"
    private var feedLimit = 10
    private var id1: Int = R.id.menuFree
    private var id2: Int = R.id.menu10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadUrl(feedURL.format(feedLimit))
        Log.d(TAG, "onCreate done")
    }

    private fun downloadUrl(feedURL: String) {
        Log.d(TAG,"downloadUrl starting AsyncTask")
        downloadData = DownloadData(this, xmlListView)
        downloadData?.execute(feedURL)
        Log.d(TAG,"downloadUrl done")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)

        if (feedLimit == 10) {
            menu?.findItem(R.id.menu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.menu25)?.isChecked = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menuFree ->
                feedURL =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/sf=143462/limit=%d/xml"
            R.id.menuPaid ->
                feedURL =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/sf=143462/limit=%d/xml"
            R.id.menuSongs ->
                feedURL =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/sf=143462/limit=%d/xml"
            R.id.menu10, R.id.menu25 -> {
                if (!item.isChecked) {
                    item.isChecked = true
                    feedLimit = 35 - feedLimit
                    Log.d(TAG, "onOptionsItemSelected: ${item.title} setting feedLimit to $feedLimit")
                } else {
                    Log.d(TAG, "onOptionsItemSelected: ${item.title} setting feedLimit unchanged")
                }
            }
            R.id.menuRefresh -> downloadUrl(feedURL.format(feedLimit))
            else ->
                return super.onOptionsItemSelected(item)
        }

        if (id1 != item.itemId && id2 != item.itemId) { //urlで判別するほうが単純
            downloadUrl(feedURL.format(feedLimit))
        }

        when (item.itemId) {
            R.id.menuFree, R.id.menuPaid, R.id.menuSongs -> id1 = item.itemId
            R.id.menu10, R.id.menu25 -> id2 = item.itemId
        }

        return true

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState?.putString("key1", feedURL)
        outState?.putInt("key2", feedLimit)
        outState?.putInt("id1", id1)
        outState?.putInt("id2", id2)

        Log.d(TAG, feedURL)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        feedURL = savedInstanceState?.getString("key1", "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/sf=143462/limit=%d/xml")
        feedLimit = savedInstanceState?.getInt("key2", 10)
        id1 = savedInstanceState?.getInt("id1", R.id.menuFree)
        id2 = savedInstanceState?.getInt("id2", R.id.menu10)

        Log.d(TAG, feedURL)
        Log.d(TAG, feedLimit.toString())

        downloadUrl(feedURL.format(feedLimit))
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    companion object {
        private class DownloadData(context: Context, listView: ListView) : AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            var propContext : Context by Delegates.notNull()
            var propListView : ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }


            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

                val feedAdapter = FeedAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter
            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error downloading")
                }
                return rssFeed
            }

            //xmlのダウンロード(この1行で良い)
            private fun downloadXML(urlPath: String?): String {
                return URL(urlPath).readText()
            }
        }
    }
}