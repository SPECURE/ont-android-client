/*******************************************************************************
 * Copyright 2014-2017 Specure GmbH
 * Copyright 2013-2015 alladin-IT GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.specure.android.screens.help;

import android.annotation.SuppressLint;
import androidx.fragment.app.FragmentActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.Fragment;

import com.specure.opennettest.R;

import at.specure.android.constants.AppConstants;
import at.specure.android.configs.ConfigHelper;

/**
 * 
 * @author
 * 
 */
public class HelpFragment extends Fragment
{
    
    // private static final String DEBUG_TAG = "HelpFragment";
    
    /**
	 * 
	 */
    public static final String ARG_URL = "url";
    
    /**
	 * 
	 */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
    {
        
        super.onCreateView(inflater, container, savedInstanceState);
        
        final Bundle args = getArguments();
        
        String url = args.getString(ARG_URL);
        
        if (url == null || url.length() == 0)
            url = this.getString(R.string.url_help);
        
        final FragmentActivity activity = getActivity();
        
        final WebView webview = new WebView(activity)
        {
            @Override
            public boolean onKeyDown(final int keyCode, final KeyEvent event)
            {
                if (keyCode == KeyEvent.KEYCODE_BACK && canGoBack())
                {
                    goBack();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
            }
        };
        
        final WebSettings webSettings = webview.getSettings();
        final String userAgent = AppConstants.getUserAgentString(getActivity());
        if (userAgent != null) {
        	webSettings.setUserAgentString(userAgent);
        }
        webSettings.setJavaScriptEnabled(true);
                
        webview.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onReceivedError(final WebView view, final int errorCode, final String description,
                    final String failingUrl)
            {
                Log.w(getTag(), "error code:" + errorCode);
                Log.d(getTag(), "error desc:" + description);
                Log.d(getTag(), "error url:" + failingUrl);
                webview.loadData(ConfigHelper.getErrorString(getContext()), "text/html", "utf-8");
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
        
        webview.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                        String contentDisposition, String mimetype,
                        long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        
        if (! url.matches("^https?://.*"))
        {
            final String protocol = ConfigHelper.isControlSeverSSL(activity) ? "https" : "http";
            url = protocol + "://" + url;
        }
        
        webview.loadUrl(url);
        
        return webview;
    }
    
}
