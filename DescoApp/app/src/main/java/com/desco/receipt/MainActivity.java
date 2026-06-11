package com.desco.receipt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;

public class MainActivity extends Activity {

    WebView hiddenWebView;
    EditText accInput;
    Button loadBtn;
    LinearLayout stepsLayout, receiptContainer, actionRow;
    ScrollView scrollView;
    Handler handler = new Handler(Looper.getMainLooper());
    String printText = "";
    String accountNo = "";

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0f1a14);

        // Header
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.VERTICAL);
        hdr.setGravity(Gravity.CENTER);
        hdr.setBackgroundColor(0xFF006b35);
        hdr.setPadding(dp(16), dp(22), dp(16), dp(18));
        TextView t1 = tv("⚡ DESCO রিসিপ্ট প্রিন্টার", 20, 0xFFFFFFFF, true);
        t1.setGravity(Gravity.CENTER);
        TextView t2 = tv("Prepaid Meter · রিয়েলটাইম রিসিপ্ট", 12, 0xFFaaddbb, false);
        t2.setGravity(Gravity.CENTER);
        t2.setPadding(0, dp(4), 0, 0);
        hdr.addView(t1); hdr.addView(t2);
        root.addView(hdr);

        // Scroll
        scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0f1a14);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(30));

        // Input card
        LinearLayout inputCard = card();
        TextView lbl = tv("অ্যাকাউন্ট নম্বর লিখুন", 11, 0xFF5a8a6a, false);
        lbl.setPadding(0,0,0,dp(8));
        inputCard.addView(lbl);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);

        accInput = new EditText(this);
        accInput.setHint("যেমন: 29147951");
        accInput.setHintTextColor(0xFF2d4a35);
        accInput.setTextColor(0xFFe8f5ee);
        accInput.setTextSize(20);
        accInput.setTypeface(Typeface.DEFAULT_BOLD);
        accInput.setBackgroundColor(0xFF0a1410);
        accInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        accInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        accInput.setSingleLine(true);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        accInput.setLayoutParams(ip);

        loadBtn = new Button(this);
        loadBtn.setText("লোড ▶");
        loadBtn.setTextColor(Color.WHITE);
        loadBtn.setTextSize(14);
        loadBtn.setTypeface(Typeface.DEFAULT_BOLD);
        loadBtn.setBackgroundColor(0xFF00a651);
        loadBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(dp(8), 0, 0, 0);
        loadBtn.setLayoutParams(bp);
        loadBtn.setOnClickListener(v -> startFetch());

        inputRow.addView(accInput);
        inputRow.addView(loadBtn);
        inputCard.addView(inputRow);

        stepsLayout = new LinearLayout(this);
        stepsLayout.setOrientation(LinearLayout.VERTICAL);
        stepsLayout.setVisibility(View.GONE);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, dp(12), 0, 0);
        stepsLayout.setLayoutParams(slp);
        inputCard.addView(stepsLayout);

        content.addView(inputCard);

        // Receipt container
        receiptContainer = new LinearLayout(this);
        receiptContainer.setOrientation(LinearLayout.VERTICAL);
        receiptContainer.setVisibility(View.GONE);
        content.addView(receiptContainer);

        // Action buttons
        actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setVisibility(View.GONE);
        LinearLayout.LayoutParams arp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arp.setMargins(0, dp(12), 0, 0);
        actionRow.setLayoutParams(arp);
        content.addView(actionRow);

        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Hidden WebView
        hiddenWebView = new WebView(this);
        hiddenWebView.setVisibility(View.GONE);
        root.addView(hiddenWebView, new LinearLayout.LayoutParams(1, 1));
        WebSettings ws = hiddenWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        hiddenWebView.addJavascriptInterface(new Bridge(), "App");

        setContentView(root);
    }

    void startFetch() {
        accountNo = accInput.getText().toString().trim();
        if (accountNo.length() < 6) {
            toast("সঠিক অ্যাকাউন্ট নম্বর দিন");
            return;
        }
        loadBtn.setEnabled(false);
        loadBtn.setText("⏳...");
        receiptContainer.setVisibility(View.GONE);
        receiptContainer.removeAllViews();
        actionRow.setVisibility(View.GONE);
        actionRow.removeAllViews();
        buildSteps();

        step(0, "active");

        // Load DESCO site then inject fetch script
        hiddenWebView.setWebViewClient(new WebViewClient() {
            boolean done = false;
            @Override
            public void onPageFinished(WebView v, String url) {
                if (!done && url.contains("desco.org.bd")) {
                    done = true;
                    handler.postDelayed(() -> injectFetch(), 2000);
                }
            }
        });
        hiddenWebView.loadUrl("https://prepaid.desco.org.bd/customer/");
    }

    @SuppressLint("SetJavaScriptEnabled")
    void injectFetch() {
        step(0, "done"); step(1, "active");
        String acc = accountNo;

        // language=JavaScript
        String js =
            "(function(){\n" +
            "  function post(url, body, cb) {\n" +
            "    var x = new XMLHttpRequest();\n" +
            "    x.open('POST', url, true);\n" +
            "    x.setRequestHeader('Content-Type','application/x-www-form-urlencoded');\n" +
            "    x.onload = function(){ cb(x.status, x.responseText); };\n" +
            "    x.onerror = function(){ cb(0, 'network error'); };\n" +
            "    x.send(body);\n" +
            "  }\n" +
            "  function get(url, token, cb) {\n" +
            "    var x = new XMLHttpRequest();\n" +
            "    x.open('GET', url, true);\n" +
            "    x.setRequestHeader('Content-Type','application/json');\n" +
            "    if(token) x.setRequestHeader('Authorization','Bearer '+token);\n" +
            "    x.onload = function(){ cb(x.status, x.responseText); };\n" +
            "    x.onerror = function(){ cb(0,'network error'); };\n" +
            "    x.send();\n" +
            "  }\n" +
            "\n" +
            "  // Step 1: Login\n" +
            "  post('/webapi/api/Token',\n" +
            "    'grant_type=password&username=" + acc + "&password=',\n" +
            "    function(status, resp) {\n" +
            "      App.log('Token status: ' + status);\n" +
            "      var token = '';\n" +
            "      try { token = JSON.parse(resp).access_token || ''; } catch(e){}\n" +
            "      App.onToken(token);\n" +
            "\n" +
            "      // Step 2: Get history\n" +
            "      var urls = [\n" +
            "        '/webapi/api/CustomerRecharge/GetRechargeHistory?accountNo=" + acc + "&pageSize=5&pageNo=1',\n" +
            "        '/webapi/api/CustomerRecharge/GetRechargeHistory?accountNo=" + acc + "&page=1&size=5',\n" +
            "        '/webapi/api/Recharge/History?accountNo=" + acc + "',\n" +
            "        '/webapi/api/Customer/RechargeHistory?accountNo=" + acc + "'\n" +
            "      ];\n" +
            "      var i = 0;\n" +
            "      function tryUrl() {\n" +
            "        if(i >= urls.length){ App.onError('সব endpoint ব্যর্থ'); return; }\n" +
            "        App.log('Trying: ' + urls[i]);\n" +
            "        get(urls[i], token, function(s, r) {\n" +
            "          App.log('URL ' + i + ' status: ' + s + ' len: ' + r.length);\n" +
            "          if(s === 200 && r.length > 10) { App.onData(r); }\n" +
            "          else { i++; tryUrl(); }\n" +
            "        });\n" +
            "      }\n" +
            "      tryUrl();\n" +
            "    }\n" +
            "  );\n" +
            "})();";

        hiddenWebView.evaluateJavascript(js, null);
    }

    class Bridge {
        @JavascriptInterface public void log(String msg) {
            android.util.Log.d("DESCO", msg);
        }
        @JavascriptInterface public void onToken(String t) {
            handler.post(() -> { step(1,"done"); step(2,"active"); });
        }
        @JavascriptInterface public void onData(String json) {
            handler.post(() -> {
                step(2,"done"); step(3,"active");
                try { parseAndRender(json); }
                catch(Exception e) { showError("Parse error: " + e.getMessage() + "\n\nRaw (200 chars):\n" + json.substring(0, Math.min(200,json.length()))); }
            });
        }
        @JavascriptInterface public void onError(String msg) {
            handler.post(() -> showError(msg));
        }
    }

    void parseAndRender(String json) throws Exception {
        json = json.trim();
        JSONArray arr = null;
        if (json.startsWith("[")) {
            arr = new JSONArray(json);
        } else {
            JSONObject obj = new JSONObject(json);
            // search all keys for array
            Iterator<String> keys = obj.keys();
            while(keys.hasNext()) {
                String k = keys.next();
                try {
                    Object v = obj.get(k);
                    if (v instanceof JSONArray && ((JSONArray)v).length() > 0) {
                        arr = (JSONArray) v; break;
                    }
                } catch(Exception ignored){}
            }
            if (arr == null) { arr = new JSONArray(); arr.put(obj); }
        }
        if (arr.length() == 0) throw new Exception("কোনো রিচার্জ পাওয়া যায়নি");

        JSONObject r = arr.getJSONObject(0);
        android.util.Log.d("DESCO", "Keys: " + r.toString());

        String name   = g(r,"customerName","CustomerName","customer_name","name","Name","CUSTOMER_NAME");
        String meter  = g(r,"meterNo","MeterNo","meter_no","meter","Meter","METER_NO","meterNumber","MeterNumber");
        String acc    = g(r,"accountNo","AccountNo","account_no","account","Account","ACCOUNT_NO"); if(acc.equals("—")) acc=accountNo;
        String date   = g(r,"rechargeDate","RechargeDate","recharge_date","date","Date","transDate","TransDate","created_at","createdAt","purchaseDate");
        String order  = g(r,"orderNo","OrderNo","order_no","orderId","OrderId","order_id","transactionId","TransactionId","transaction_id");
        String seq    = g(r,"sequence","Sequence","seq","Seq","sequenceNo","SequenceNo");
        String oper   = g(r,"rechargeOperator","RechargeOperator","operator","Operator","channel","Channel","paymentMode","PaymentMode");
        String energy = g(r,"energyCost","EnergyCost","energy_cost","energy","Energy","unitCost","UnitCost","kwh_cost");
        String demand = g(r,"demandCharge","DemandCharge","demand_charge","demand"); if(demand.equals("—")) demand="0";
        String rent   = g(r,"meterRent","MeterRent","meter_rent"); if(rent.equals("—")) rent="0";
        String vat    = g(r,"vat","Vat","VAT","vatAmount","VatAmount","vat_amount","tax","Tax");
        String rebate = g(r,"rebate","Rebate","discount","Discount"); if(rebate.equals("—")) rebate="0";
        String gross  = g(r,"grossAmount","GrossAmount","gross_amount","amount","Amount","totalAmount","TotalAmount","total","Total","paidAmount","PaidAmount");
        String token  = g(r,"token","Token","rechargeToken","RechargeToken","recharge_token","tokenNo","TokenNo","tokenNumber","TokenNumber","mstoken");

        step(3,"done");
        buildReceipt(name,meter,acc,date,order,seq,oper,energy,demand,rent,vat,rebate,gross,token);
        loadBtn.setEnabled(true); loadBtn.setText("লোড ▶");
        handler.postDelayed(() -> scrollView.smoothScrollTo(0, receiptContainer.getTop()), 300);
    }

    String g(JSONObject o, String... keys) {
        for(String k: keys) {
            if(o.has(k)) {
                try {
                    String v = o.getString(k);
                    if(v!=null && !v.equals("null") && !v.trim().isEmpty()) return v.trim();
                } catch(Exception ignored){}
            }
        }
        return "—";
    }

    void buildReceipt(String name, String meter, String acc, String date,
                      String order, String seq, String oper, String energy,
                      String demand, String rent, String vat, String rebate,
                      String gross, String token) {
        receiptContainer.removeAllViews();

        // White receipt card
        LinearLayout rc = new LinearLayout(this);
        rc.setOrientation(LinearLayout.VERTICAL);
        rc.setBackgroundColor(Color.WHITE);

        // Green header
        LinearLayout rh = new LinearLayout(this);
        rh.setOrientation(LinearLayout.VERTICAL);
        rh.setGravity(Gravity.CENTER);
        rh.setBackgroundColor(0xFF006b35);
        rh.setPadding(dp(16),dp(18),dp(16),dp(18));
        rh.addView(ctv("Dhaka Electricity Supply PLC", 16, Color.WHITE, true));
        rh.addView(ctv("DESCO — Prepaid Recharge", 12, 0xFFaaddbb, false));
        TextView badge = ctv("✓  EXECUTION SUCCESSFUL", 11, Color.WHITE, true);
        badge.setBackgroundColor(0xFF004d25);
        badge.setPadding(dp(14),dp(5),dp(14),dp(5));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0,dp(10),0,0); blp.gravity = Gravity.CENTER;
        badge.setLayoutParams(blp);
        rh.addView(badge);
        rc.addView(rh);

        // Body
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(Color.WHITE);
        body.setPadding(dp(16),dp(14),dp(16),dp(14));

        secTitle(body,"ট্রানজেকশন তথ্য");
        rrow(body,"তারিখ ও সময়",date);
        rrow(body,"অর্ডার নম্বর",order);
        rrow(body,"সিকোয়েন্স",seq);
        rrow(body,"অপারেটর",oper);

        secTitle(body,"গ্রাহক তথ্য");
        rrow(body,"নাম",name);
        rrow(body,"মিটার নম্বর",meter);
        rrow(body,"অ্যাকাউন্ট নম্বর",acc);

        secTitle(body,"বিল বিভাজন");
        rrow(body,"বিদ্যুৎ খরচ",energy);
        rrow(body,"Demand Charge",demand);
        rrow(body,"Meter Rent",rent);
        rrow(body,"ভ্যাট (৫%)",vat);
        rrow(body,"রিবেট",rebate);

        // Gross box
        LinearLayout gb = new LinearLayout(this);
        gb.setOrientation(LinearLayout.HORIZONTAL);
        gb.setBackgroundColor(0xFFe8f5ee);
        gb.setPadding(dp(14),dp(14),dp(14),dp(14));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0,dp(12),0,dp(12)); gb.setLayoutParams(glp);
        TextView gl2 = tv("মোট পরিশোধ",14,0xFF007a3d,true);
        gl2.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        gb.addView(gl2);
        gb.addView(tv("৳ "+gross,24,0xFF003d1f,true));
        body.addView(gb);

        // Token box
        LinearLayout tb = new LinearLayout(this);
        tb.setOrientation(LinearLayout.VERTICAL);
        tb.setGravity(Gravity.CENTER);
        tb.setBackgroundColor(0xFF0f1a14);
        tb.setPadding(dp(16),dp(16),dp(16),dp(16));
        tb.addView(ctv("⚡  RECHARGE TOKEN",9,0xFF3d6b4d,true));
        TextView tokTv = ctv(token,22,0xFF4cef8a,true);
        tokTv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tokTv.setPadding(0,dp(8),0,0);
        tb.addView(tokTv);
        body.addView(tb);

        rc.addView(body);
        receiptContainer.addView(rc);
        receiptContainer.setVisibility(View.VISIBLE);

        // Print text
        String D="================================", L="--------------------------------";
        printText = D+"\n  Dhaka Electricity Supply PLC\n        DESCO PREPAID RECEIPT\n"+D
            +"\nDate  : "+date+"\nOrder : "+order+"\nSeq   : "+seq+"\nOper  : "+oper
            +"\n"+L+"\nName  : "+name+"\nMeter : "+meter+"\nAcct  : "+acc
            +"\n"+L+"\nEnergy: "+energy+"\nDemand: "+demand+"\nRent  : "+rent
            +"\nVAT 5%: "+vat+"\nRebate: "+rebate+"\n"+D
            +"\nGROSS : BDT "+gross+"\n"+D+"\n\n      *** RECHARGE TOKEN ***\n  "+token
            +"\n\n"+D+"\n   ** EXECUTION SUCCESSFUL **\n"+D+"\n\n\n";

        // Action buttons
        actionRow.removeAllViews();
        Button pb = new Button(this);
        pb.setText("🖨️  Print / Share");
        pb.setTextColor(Color.WHITE);
        pb.setBackgroundColor(0xFF1a2e22);
        pb.setTextSize(14); pb.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams pblp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        pblp.setMargins(0,0,dp(6),0); pb.setLayoutParams(pblp);
        pb.setOnClickListener(v -> shareText());

        Button rb2 = new Button(this);
        rb2.setText("📲  RawBT Print");
        rb2.setTextColor(Color.WHITE);
        rb2.setBackgroundColor(0xFF00a651);
        rb2.setTextSize(14); rb2.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams rblp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        rblp.setMargins(dp(6),0,0,0); rb2.setLayoutParams(rblp);
        rb2.setOnClickListener(v -> rawbt());

        actionRow.addView(pb); actionRow.addView(rb2);
        actionRow.setVisibility(View.VISIBLE);
    }

    void rawbt() {
        if (printText.isEmpty()) return;
        try {
            Intent i = new Intent("com.android.rawbt.action.PRINT");
            i.putExtra("text", printText);
            startActivity(i);
        } catch(Exception e) { shareText(); }
    }

    void shareText() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, printText);
        i.putExtra(Intent.EXTRA_SUBJECT, "DESCO Receipt - " + accountNo);
        startActivity(Intent.createChooser(i, "শেয়ার / প্রিন্ট করুন"));
    }

    // ── Steps ────────────────────────────────────────
    String[] stepLabels = {"🔗  DESCO সার্ভারে সংযোগ","🔐  অ্যাকাউন্ট যাচাই","📋  রিচার্জ ইতিহাস লোড","🧾  রিসিপ্ট তৈরি"};

    void buildSteps() {
        stepsLayout.removeAllViews();
        stepsLayout.setVisibility(View.VISIBLE);
        for (String l : stepLabels) {
            TextView s = tv("● " + l, 13, 0xFF3d6b4d, false);
            s.setBackgroundColor(0xFF0f1a14);
            s.setPadding(dp(12),dp(10),dp(12),dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,dp(4),0,0); s.setLayoutParams(lp);
            stepsLayout.addView(s);
        }
    }

    void step(int i, String state) {
        if(stepsLayout.getChildCount()<=i) return;
        TextView s = (TextView)stepsLayout.getChildAt(i);
        if(state.equals("active")){s.setTextColor(0xFF00a651);s.setBackgroundColor(0xFF0d2a18);}
        else if(state.equals("done")){s.setTextColor(0xFF4cef8a);s.setBackgroundColor(0xFF0d2a18);}
        else if(state.equals("err")){s.setTextColor(0xFFef4c4c);s.setBackgroundColor(0xFF2a0d0d);}
    }

    void showError(String msg) {
        loadBtn.setEnabled(true); loadBtn.setText("লোড ▶");
        for(int i=0;i<4;i++) step(i,"err");
        toast(msg);
        android.util.Log.e("DESCO","Error: "+msg);
    }

    // ── UI Helpers ───────────────────────────────────
    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(0xFF1a2e22);
        c.setPadding(dp(16),dp(16),dp(16),dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,dp(12)); c.setLayoutParams(lp);
        return c;
    }
    TextView tv(String t, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(size); v.setTextColor(color);
        if(bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }
    TextView ctv(String t, int size, int color, boolean bold) {
        TextView v = tv(t,size,color,bold); v.setGravity(Gravity.CENTER); return v;
    }
    void secTitle(LinearLayout p, String title) {
        TextView v = tv(title,10,0xFF8aab96,true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,dp(14),0,dp(6)); v.setLayoutParams(lp);
        p.addView(v);
        View line = new View(this); line.setBackgroundColor(0xFFe8f0eb);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1)));
        p.addView(line);
    }
    void rrow(LinearLayout p, String label, String val) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0,dp(5),0,dp(5));
        TextView l = tv(label,13,0xFF7a9a84,false);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        TextView v = tv(val,13,0xFF1a2e22,true);
        v.setGravity(Gravity.END);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        row.addView(l); row.addView(v); p.addView(row);
    }
    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
