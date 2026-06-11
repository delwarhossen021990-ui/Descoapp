package com.desco.receipt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText accInput;
    private Button loadBtn;
    private LinearLayout receiptLayout;
    private ScrollView scrollView;
    private TextView statusText;
    private LinearLayout stepsLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String currentReceiptText = "";
    private String currentAccountNo = "";

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        buildUI();

        // Hidden WebView for API calls
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.addJavascriptInterface(new JSBridge(), "AndroidBridge");
        ((LinearLayout) scrollView.getParent()).addView(webView, 0,
                new LinearLayout.LayoutParams(1, 1));
    }

    // ─── BUILD UI PROGRAMMATICALLY ───────────────────
    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0f1a14"));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setBackgroundColor(Color.parseColor("#006b35"));
        header.setPadding(dp(20), dp(24), dp(20), dp(20));
        TextView h1 = new TextView(this);
        h1.setText("⚡ DESCO রিসিপ্ট প্রিন্টার");
        h1.setTextSize(20); h1.setTextColor(Color.WHITE);
        h1.setTypeface(null, android.graphics.Typeface.BOLD);
        h1.setGravity(Gravity.CENTER);
        TextView h2 = new TextView(this);
        h2.setText("Prepaid Meter · রিয়েলটাইম রিসিপ্ট");
        h2.setTextSize(12); h2.setTextColor(Color.parseColor("#aaddbb"));
        h2.setGravity(Gravity.CENTER); h2.setPadding(0, dp(4), 0, 0);
        header.addView(h1); header.addView(h2);
        root.addView(header);

        // Main scroll
        scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#0f1a14"));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(30));

        // Input card
        LinearLayout inputCard = card();
        TextView lbl = new TextView(this);
        lbl.setText("অ্যাকাউন্ট নম্বর লিখুন");
        lbl.setTextSize(11); lbl.setTextColor(Color.parseColor("#5a8a6a"));
        lbl.setPadding(0, 0, 0, dp(8));
        inputCard.addView(lbl);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        accInput = new EditText(this);
        accInput.setHint("যেমন: 29147951");
        accInput.setHintTextColor(Color.parseColor("#3d6b4d"));
        accInput.setTextColor(Color.parseColor("#e8f5ee"));
        accInput.setTextSize(20);
        accInput.setTypeface(null, android.graphics.Typeface.BOLD);
        accInput.setBackgroundColor(Color.parseColor("#0a1410"));
        accInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        accInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        accInput.setSingleLine(true);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        accInput.setLayoutParams(ip);

        loadBtn = new Button(this);
        loadBtn.setText("লোড ▶");
        loadBtn.setTextColor(Color.WHITE);
        loadBtn.setTextSize(14);
        loadBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        loadBtn.setBackgroundColor(Color.parseColor("#00a651"));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(dp(8), 0, 0, 0);
        loadBtn.setLayoutParams(bp);
        loadBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        loadBtn.setOnClickListener(v -> startFetch());

        row.addView(accInput); row.addView(loadBtn);
        inputCard.addView(row);

        statusText = new TextView(this);
        statusText.setTextSize(13); statusText.setVisibility(View.GONE);
        statusText.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(0, dp(10), 0, 0);
        statusText.setLayoutParams(sp);
        inputCard.addView(statusText);

        stepsLayout = new LinearLayout(this);
        stepsLayout.setOrientation(LinearLayout.VERTICAL);
        stepsLayout.setVisibility(View.GONE);
        inputCard.addView(stepsLayout);

        content.addView(inputCard);

        // Receipt layout
        receiptLayout = new LinearLayout(this);
        receiptLayout.setOrientation(LinearLayout.VERTICAL);
        receiptLayout.setVisibility(View.GONE);
        content.addView(receiptLayout);

        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    // ─── START FETCH PROCESS ─────────────────────────
    private void startFetch() {
        currentAccountNo = accInput.getText().toString().trim();
        if (currentAccountNo.length() < 6) {
            showStatus("সঠিক অ্যাকাউন্ট নম্বর দিন", "#ef4c4c");
            return;
        }
        loadBtn.setEnabled(false);
        loadBtn.setText("⏳...");
        receiptLayout.setVisibility(View.GONE);
        receiptLayout.removeAllViews();
        showSteps();
        setStepState(0, "active");

        // Load DESCO login page in hidden WebView, then inject JS
        webView.setWebViewClient(new WebViewClient() {
            boolean injected = false;
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!injected && url.contains("desco.org.bd")) {
                    injected = true;
                    handler.postDelayed(() -> injectLoginScript(), 1500);
                }
            }
        });
        webView.loadUrl("https://prepaid.desco.org.bd/customer/#/customer-login");
    }

    // ─── INJECT LOGIN + DATA FETCH SCRIPT ────────────
    private void injectLoginScript() {
        setStepState(0, "done"); setStepState(1, "active");
        String acc = currentAccountNo;
        String js = "javascript:(function(){\n" +
            "  var xhr = new XMLHttpRequest();\n" +
            "  xhr.open('POST', '/webapi/api/Token', true);\n" +
            "  xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');\n" +
            "  xhr.onreadystatechange = function() {\n" +
            "    if(xhr.readyState === 4) {\n" +
            "      if(xhr.status === 200) {\n" +
            "        try {\n" +
            "          var res = JSON.parse(xhr.responseText);\n" +
            "          var token = res.access_token;\n" +
            "          AndroidBridge.onToken(token);\n" +
            "          fetchHistory(token);\n" +
            "        } catch(e) { AndroidBridge.onError('Login parse error: ' + e); }\n" +
            "      } else {\n" +
            "        // Try without token\n" +
            "        fetchHistory('');\n" +
            "      }\n" +
            "    }\n" +
            "  };\n" +
            "  xhr.send('grant_type=password&username=" + acc + "&password=');\n" +
            "\n" +
            "  function fetchHistory(token) {\n" +
            "    var xhr2 = new XMLHttpRequest();\n" +
            "    var url = '/webapi/api/CustomerRecharge/GetRechargeHistory?accountNo=" + acc + "&pageSize=5&pageNo=1';\n" +
            "    xhr2.open('GET', url, true);\n" +
            "    xhr2.setRequestHeader('Content-Type', 'application/json');\n" +
            "    if(token) xhr2.setRequestHeader('Authorization', 'Bearer ' + token);\n" +
            "    xhr2.onreadystatechange = function() {\n" +
            "      if(xhr2.readyState === 4) {\n" +
            "        if(xhr2.status === 200) {\n" +
            "          AndroidBridge.onHistory(xhr2.responseText);\n" +
            "        } else {\n" +
            "          // Try alternative endpoint\n" +
            "          fetchAlt(token);\n" +
            "        }\n" +
            "      }\n" +
            "    };\n" +
            "    xhr2.send();\n" +
            "  }\n" +
            "\n" +
            "  function fetchAlt(token) {\n" +
            "    var endpoints = [\n" +
            "      '/webapi/api/CustomerRecharge/GetLastRecharge?accountNo=" + acc + "',\n" +
            "      '/webapi/api/Recharge/GetHistory?accountNo=" + acc + "&page=1&pageSize=5',\n" +
            "      '/webapi/api/Customer/GetRechargeHistory?accountNo=" + acc + "'\n" +
            "    ];\n" +
            "    var i = 0;\n" +
            "    function tryNext() {\n" +
            "      if(i >= endpoints.length) { AndroidBridge.onError('All endpoints failed'); return; }\n" +
            "      var x = new XMLHttpRequest();\n" +
            "      x.open('GET', endpoints[i], true);\n" +
            "      if(token) x.setRequestHeader('Authorization', 'Bearer ' + token);\n" +
            "      x.onreadystatechange = function() {\n" +
            "        if(x.readyState === 4) {\n" +
            "          if(x.status === 200) { AndroidBridge.onHistory(x.responseText); }\n" +
            "          else { i++; tryNext(); }\n" +
            "        }\n" +
            "      };\n" +
            "      x.send();\n" +
            "    }\n" +
            "    tryNext();\n" +
            "  }\n" +
            "})();";
        webView.loadUrl(js);
    }

    // ─── JS BRIDGE ────────────────────────────────────
    class JSBridge {
        @JavascriptInterface
        public void onToken(String token) {
            handler.post(() -> setStepState(1, "done"));
        }

        @JavascriptInterface
        public void onHistory(final String json) {
            handler.post(() -> {
                setStepState(2, "done"); setStepState(3, "active");
                try {
                    parseAndShowReceipt(json);
                } catch (Exception e) {
                    onError("Parse error: " + e.getMessage() + "\nRaw: " + json.substring(0, Math.min(200, json.length())));
                }
            });
        }

        @JavascriptInterface
        public void onError(final String msg) {
            handler.post(() -> {
                loadBtn.setEnabled(true); loadBtn.setText("লোড ▶");
                showStatus("❌ " + msg, "#ef4c4c");
            });
        }
    }

    // ─── PARSE JSON & SHOW RECEIPT ───────────────────
    private void parseAndShowReceipt(String json) throws Exception {
        JSONArray arr = null;
        JSONObject obj = null;

        json = json.trim();
        if (json.startsWith("[")) {
            arr = new JSONArray(json);
        } else if (json.startsWith("{")) {
            obj = new JSONObject(json);
            // Try common wrapper keys
            for (String key : new String[]{"data","Data","rechargeList","RechargeList","result","Result"}) {
                if (obj.has(key)) {
                    Object v = obj.get(key);
                    if (v instanceof JSONArray) { arr = (JSONArray) v; break; }
                }
            }
            if (arr == null) {
                // Single record
                arr = new JSONArray(); arr.put(obj);
            }
        }

        if (arr == null || arr.length() == 0) {
            throw new Exception("কোনো রিচার্জ রেকর্ড পাওয়া যায়নি");
        }

        setStepState(3, "done");

        JSONObject r = arr.getJSONObject(0);
        String name     = g(r, "customerName","CustomerName","name","Name");
        String meter    = g(r, "meterNo","MeterNo","meter","Meter");
        String accNo    = g(r, "accountNo","AccountNo"); if(accNo.equals("—")) accNo = currentAccountNo;
        String date     = g(r, "rechargeDate","RechargeDate","date","Date","transDate","TransDate");
        String order    = g(r, "orderNo","OrderNo","orderId","OrderId");
        String seq      = g(r, "sequence","Sequence","seq");
        String oper     = g(r, "rechargeOperator","RechargeOperator","operator","Operator");
        String energy   = g(r, "energyCost","EnergyCost","energy","Energy");
        String demand   = g(r, "demandCharge","DemandCharge"); if(demand.equals("—")) demand="0";
        String rent     = g(r, "meterRent","MeterRent","rent"); if(rent.equals("—")) rent="0";
        String vat      = g(r, "vat","Vat","vatAmount","VatAmount");
        String rebate   = g(r, "rebate","Rebate");
        String gross    = g(r, "grossAmount","GrossAmount","amount","Amount","totalAmount");
        String token    = g(r, "token","Token","rechargeToken","RechargeToken");

        buildReceiptUI(name, meter, accNo, date, order, seq, oper, energy, demand, rent, vat, rebate, gross, token);

        // Build print text
        currentReceiptText = buildPrintText(name, meter, accNo, date, order, seq, oper, energy, demand, rent, vat, rebate, gross, token);

        loadBtn.setEnabled(true); loadBtn.setText("লোড ▶");
        statusText.setVisibility(View.GONE);

        handler.postDelayed(() ->
            scrollView.smoothScrollTo(0, receiptLayout.getTop()), 300);
    }

    private String g(JSONObject obj, String... keys) {
        for (String k : keys) {
            if (obj.has(k)) {
                try {
                    String v = obj.getString(k);
                    if (v != null && !v.equals("null") && !v.isEmpty()) return v;
                } catch (Exception ignored) {}
            }
        }
        return "—";
    }

    // ─── BUILD RECEIPT UI ────────────────────────────
    private void buildReceiptUI(String name, String meter, String accNo,
                                 String date, String order, String seq, String oper,
                                 String energy, String demand, String rent,
                                 String vat, String rebate, String gross, String token) {
        receiptLayout.removeAllViews();

        // Receipt card
        LinearLayout rc = new LinearLayout(this);
        rc.setOrientation(LinearLayout.VERTICAL);
        rc.setBackgroundColor(Color.WHITE);

        // Head
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER);
        head.setBackgroundColor(Color.parseColor("#006b35"));
        head.setPadding(dp(20), dp(18), dp(20), dp(18));
        addText(head, "Dhaka Electricity Supply PLC", 16, Color.WHITE, true);
        addText(head, "DESCO — Prepaid Recharge", 12, Color.parseColor("#aaddbb"), false);
        TextView badge = new TextView(this);
        badge.setText("✓  EXECUTION SUCCESSFUL");
        badge.setTextSize(11); badge.setTextColor(Color.WHITE);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setBackgroundColor(Color.parseColor("#004d25"));
        badge.setPadding(dp(14), dp(5), dp(14), dp(5));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, dp(10), 0, 0); blp.gravity = Gravity.CENTER;
        badge.setLayoutParams(blp);
        head.addView(badge);
        rc.addView(head);

        // Body
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(16), dp(18), dp(16));
        body.setBackgroundColor(Color.WHITE);

        addSectionTitle(body, "ট্রানজেকশন তথ্য");
        addRow(body, "তারিখ ও সময়", date);
        addRow(body, "অর্ডার নম্বর", order);
        addRow(body, "সিকোয়েন্স", seq);
        addRow(body, "অপারেটর", oper);

        addSectionTitle(body, "গ্রাহক তথ্য");
        addRow(body, "নাম", name);
        addRow(body, "মিটার নম্বর", meter);
        addRow(body, "অ্যাকাউন্ট নম্বর", accNo);

        addSectionTitle(body, "বিল বিভাজন");
        addRow(body, "বিদ্যুৎ খরচ", energy);
        addRow(body, "Demand Charge", demand);
        addRow(body, "Meter Rent", rent);
        addRow(body, "ভ্যাট (৫%)", vat);
        addRow(body, "রিবেট", rebate);

        // Gross
        LinearLayout grossBox = new LinearLayout(this);
        grossBox.setOrientation(LinearLayout.HORIZONTAL);
        grossBox.setBackgroundColor(Color.parseColor("#e8f5ee"));
        grossBox.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0, dp(12), 0, dp(12)); grossBox.setLayoutParams(glp);
        TextView gl = new TextView(this); gl.setText("মোট পরিশোধ");
        gl.setTextSize(14); gl.setTextColor(Color.parseColor("#007a3d"));
        gl.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams gllp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        gl.setLayoutParams(gllp);
        TextView gv = new TextView(this); gv.setText("৳ " + gross);
        gv.setTextSize(24); gv.setTextColor(Color.parseColor("#003d1f"));
        gv.setTypeface(null, android.graphics.Typeface.BOLD);
        grossBox.addView(gl); grossBox.addView(gv);
        body.addView(grossBox);

        // Token
        LinearLayout tokenBox = new LinearLayout(this);
        tokenBox.setOrientation(LinearLayout.VERTICAL);
        tokenBox.setGravity(Gravity.CENTER);
        tokenBox.setBackgroundColor(Color.parseColor("#0f1a14"));
        tokenBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        addText(tokenBox, "⚡  RECHARGE TOKEN", 9, Color.parseColor("#3d6b4d"), true);
        TextView tv = new TextView(this); tv.setText(token);
        tv.setTextSize(20); tv.setTextColor(Color.parseColor("#4cef8a"));
        tv.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER); tv.setPadding(0, dp(8), 0, 0);
        tv.setLetterSpacing(0.1f);
        tokenBox.addView(tv);
        body.addView(tokenBox);

        rc.addView(body);
        receiptLayout.addView(rc);

        // Action buttons
        LinearLayout actRow = new LinearLayout(this);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams arlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arlp.setMargins(0, dp(12), 0, 0); actRow.setLayoutParams(arlp);
        actRow.setWeightSum(2);

        Button printBtn = new Button(this);
        printBtn.setText("🖨️  Print");
        printBtn.setTextColor(Color.parseColor("#4cef8a"));
        printBtn.setBackgroundColor(Color.parseColor("#0f1a14"));
        printBtn.setTextSize(14);
        printBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams pb = new LinearLayout.LayoutParams(0, dp(52), 1f);
        pb.setMargins(0, 0, dp(6), 0); printBtn.setLayoutParams(pb);
        printBtn.setOnClickListener(v -> doPrint());

        Button rawbtBtn = new Button(this);
        rawbtBtn.setText("📲  RawBT Share");
        rawbtBtn.setTextColor(Color.WHITE);
        rawbtBtn.setBackgroundColor(Color.parseColor("#00a651"));
        rawbtBtn.setTextSize(14);
        rawbtBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams rb = new LinearLayout.LayoutParams(0, dp(52), 1f);
        rb.setMargins(dp(6), 0, 0, 0); rawbtBtn.setLayoutParams(rb);
        rawbtBtn.setOnClickListener(v -> doRawBT());

        actRow.addView(printBtn); actRow.addView(rawbtBtn);
        receiptLayout.addView(actRow);
        receiptLayout.setVisibility(View.VISIBLE);
    }

    // ─── PRINT TEXT ──────────────────────────────────
    private String buildPrintText(String name, String meter, String accNo,
                                   String date, String order, String seq, String oper,
                                   String energy, String demand, String rent,
                                   String vat, String rebate, String gross, String token) {
        String D = "================================";
        String L = "--------------------------------";
        return D + "\n" +
               "  Dhaka Electricity Supply PLC\n" +
               "        DESCO PREPAID RECEIPT\n" + D + "\n" +
               "Date  : " + date + "\n" +
               "Order : " + order + "\n" +
               "Seq   : " + seq + "\n" +
               "Oper  : " + oper + "\n" + L + "\n" +
               "Name  : " + name + "\n" +
               "Meter : " + meter + "\n" +
               "Acct  : " + accNo + "\n" + L + "\n" +
               "Energy: " + energy + "\n" +
               "Demand: " + demand + "\n" +
               "Rent  : " + rent + "\n" +
               "VAT 5%: " + vat + "\n" +
               "Rebate: " + rebate + "\n" + D + "\n" +
               "GROSS : BDT " + gross + "\n" + D + "\n\n" +
               "      *** RECHARGE TOKEN ***\n" +
               "  " + token + "\n\n" + D + "\n" +
               "   ** EXECUTION SUCCESSFUL **\n" + D + "\n\n\n";
    }

    // ─── RAWBT ───────────────────────────────────────
    private void doRawBT() {
        if (currentReceiptText.isEmpty()) return;
        try {
            Intent intent = new Intent("com.android.rawbt.action.PRINT");
            intent.putExtra("text", currentReceiptText);
            startActivity(intent);
        } catch (Exception e) {
            // Fallback: share as plain text
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, currentReceiptText);
            share.putExtra(Intent.EXTRA_SUBJECT, "DESCO Receipt");
            startActivity(Intent.createChooser(share, "RawBT দিয়ে প্রিন্ট করুন"));
        }
    }

    private void doPrint() {
        if (currentReceiptText.isEmpty()) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, currentReceiptText);
        startActivity(Intent.createChooser(share, "প্রিন্ট / শেয়ার করুন"));
    }

    // ─── UI HELPERS ──────────────────────────────────
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(Color.parseColor("#1a2e22"));
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14)); c.setLayoutParams(lp);
        return c;
    }

    private void addText(LinearLayout parent, String txt, int size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(size); tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(tv);
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView tv = new TextView(this);
        tv.setText(title); tv.setTextSize(10); tv.setTextColor(Color.parseColor("#8aab96"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(14), 0, dp(6)); tv.setLayoutParams(lp);
        parent.addView(tv);
        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#e8f0eb"));
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        parent.addView(line);
    }

    private void addRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView lv = new TextView(this); lv.setText(label);
        lv.setTextSize(13); lv.setTextColor(Color.parseColor("#7a9a84"));
        lv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView rv = new TextView(this); rv.setText(value);
        rv.setTextSize(13); rv.setTextColor(Color.parseColor("#1a2e22"));
        rv.setTypeface(null, android.graphics.Typeface.BOLD);
        rv.setGravity(Gravity.END);
        rv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lv); row.addView(rv);
        parent.addView(row);
    }

    private void showStatus(String msg, String color) {
        statusText.setText(msg);
        statusText.setTextColor(Color.parseColor(color));
        statusText.setBackgroundColor(Color.parseColor(color.equals("#ef4c4c") ? "#2a0d0d" : "#0d2a18"));
        statusText.setPadding(dp(12), dp(10), dp(12), dp(10));
        statusText.setVisibility(View.VISIBLE);
    }

    private void showSteps() {
        stepsLayout.removeAllViews();
        stepsLayout.setVisibility(View.VISIBLE);
        String[] labels = {
            "🔗  DESCO সার্ভারে সংযোগ",
            "🔐  অ্যাকাউন্ট যাচাই",
            "📋  রিচার্জ ইতিহাস লোড",
            "🧾  রিসিপ্ট তৈরি"
        };
        for (String l : labels) {
            TextView tv = new TextView(this);
            tv.setText("● " + l);
            tv.setTextSize(13); tv.setTextColor(Color.parseColor("#3d6b4d"));
            tv.setTag(l);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), 0, 0); tv.setLayoutParams(lp);
            tv.setBackgroundColor(Color.parseColor("#0f1a14"));
            tv.setPadding(dp(12), dp(10), dp(12), dp(10));
            stepsLayout.addView(tv);
        }
    }

    private void setStepState(int idx, String state) {
        if (stepsLayout.getChildCount() <= idx) return;
        TextView tv = (TextView) stepsLayout.getChildAt(idx);
        switch(state) {
            case "active": tv.setTextColor(Color.parseColor("#00a651")); tv.setBackgroundColor(Color.parseColor("#0d2a18")); break;
            case "done":   tv.setTextColor(Color.parseColor("#4cef8a")); tv.setBackgroundColor(Color.parseColor("#0d2a18")); break;
            case "err":    tv.setTextColor(Color.parseColor("#ef4c4c")); tv.setBackgroundColor(Color.parseColor("#2a0d0d")); break;
        }
    }

    private int dp(int val) {
        return (int)(val * getResources().getDisplayMetrics().density);
    }
}
