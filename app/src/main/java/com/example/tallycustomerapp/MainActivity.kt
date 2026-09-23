package com.example.tallycustomerapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tallycustomerapp.data.AppDatabase
import com.example.tallycustomerapp.databinding.ActivityMainBinding
import com.example.tallycustomerapp.web.WebAppInterface
import android.content.Context
import android.view.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tallycustomerapp.data.OfflineCompany
import com.example.tallycustomerapp.databinding.FragmentOfflineBinding
import com.example.tallycustomerapp.databinding.ItemCompanyBinding
import com.example.tallycustomerapp.offline.OfflineViewModel
import com.example.tallycustomerapp.offline.OfflineViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
    }

    private fun setupNavigation() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LivePortalFragment())
            .commit()
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.tab_live -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LivePortalFragment())
                        .commit()
                    true
                }
                R.id.tab_offline -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, OfflineFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    class LivePortalFragment : Fragment() {
        private lateinit var db: AppDatabase

        @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View {
            val view = inflater.inflate(R.layout.fragment_live, container, false)
            val webView: WebView = view.findViewById(R.id.webView)
            db = AppDatabase.getDatabase(requireContext())

            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.setSupportMultipleWindows(false)
            webView.settings.userAgentString = webView.settings.userAgentString + " AndroidNativeWebView"

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            restoreCookies(requireContext(), cookieManager)

            webView.addJavascriptInterface(
                WebAppInterface(requireContext(), db),
                "AndroidBridge"
            )

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    saveCookies(requireContext(), cookieManager)
                    injectCompanySyncJs(webView)
                }
            }

            webView.loadUrl("https://customer.tallysolutions.com/customerapp/")
            return view
        }

        private fun injectCompanySyncJs(webView: WebView) {
            val js = """
                (function() {
                    const buttonStyle = 'background:#2196f3;color:#fff;border:none;padding:6px 14px;border-radius:5px;margin-left:6px;cursor:pointer;';
                    function findCompanyTable() {
                        const tables = document.getElementsByTagName('table');
                        for (let t of tables) {
                            let header = t.querySelector('thead');
                            if (header && /Company\s*Name/i.test(header.innerText) && /Connection\s*Status/i.test(header.innerText))
                                return t;
                        }
                        return null;
                    }
                    function injectButtons() {
                        const table = findCompanyTable();
                        if (!table) return;
                        const rows = table.querySelectorAll('tbody tr');
                        for (let row of rows) {
                            let statusCell = null, actionsCell = null, companyCell = null, snCell = null;
                            let cells = row.querySelectorAll('td');
                            companyCell = cells[1];
                            snCell = cells[2];
                            statusCell = Array.from(cells).find(td => /CONNECTED/i.test(td.textContent)) || cells[4];
                            actionsCell = cells[cells.length-1];
                            if (statusCell && /CONNECTED/i.test(statusCell.innerText) && !row.hasAttribute('data-synced')) {
                                row.setAttribute('data-synced', '1');
                                const btn = document.createElement('button');
                                btn.textContent = 'Sync Offline';
                                btn.style = buttonStyle;
                                btn.onclick = function(e) {
                                    e.stopPropagation();
                                    e.preventDefault();
                                    const companyName = companyCell ? companyCell.innerText.trim() : '';
                                    const serialNumber = snCell ? snCell.innerText.trim() : '';
                                    const ledgerList = [
                                        { name: "Sales", amount: Math.floor(Math.random()*10000+1000) },
                                        { name: "Receipts", amount: Math.floor(Math.random()*10000+1000) }
                                    ];
                                    const voucherList = [
                                        { date: "2024-05-01", type: "Receipt", amount: 1500 },
                                        { date: "2024-04-20", type: "Payment", amount: 700 }
                                    ];
                                    const payload = JSON.stringify({
                                        companyName: companyName,
                                        serialNumber: serialNumber,
                                        ledgerList: ledgerList,
                                        voucherList: voucherList
                                    });
                                    if (window.AndroidBridge && window.AndroidBridge.saveCompanyDataOffline) {
                                        window.AndroidBridge.saveCompanyDataOffline(payload);
                                    }
                                };
                                actionsCell && actionsCell.appendChild(btn);
                            }
                        }
                    }
                    const observer = new MutationObserver(function() {
                        try { injectButtons(); } catch (e) {}
                    });
                    observer.observe(document.body, { childList: true, subtree: true });
                    setTimeout(injectButtons, 1500);
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }

        private fun saveCookies(context: Context, cookieManager: CookieManager) {
            val cookies = cookieManager.getCookie("https://customer.tallysolutions.com/customerapp/")
            context.getSharedPreferences("cookies", Context.MODE_PRIVATE).edit()
                .putString("tally_cookies", cookies)
                .apply()
        }

        private fun restoreCookies(context: Context, cookieManager: CookieManager) {
            val cookies = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
                .getString("tally_cookies", null)
            if (!cookies.isNullOrEmpty()) {
                cookieManager.setCookie("https://customer.tallysolutions.com/customerapp/", cookies)
                CookieManager.getInstance().flush()
            }
        }
    }

    class OfflineFragment : Fragment() {
        private var _binding: FragmentOfflineBinding? = null
        private val binding get() = _binding!!
        private lateinit var adapter: CompanyAdapter
        private lateinit var viewModel: OfflineViewModel

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View {
            _binding = FragmentOfflineBinding.inflate(inflater, container, false)
            val db = AppDatabase.getDatabase(requireContext())
            viewModel = ViewModelProvider(this, OfflineViewModelFactory(db.companyDao()))
                .get(OfflineViewModel::class.java)
            setupRecyclerView()
            observeViewModel()
            return binding.root
        }

        override fun onResume() {
            super.onResume()
            viewModel.loadCompanies()
        }

        private fun setupRecyclerView() {
            adapter = CompanyAdapter { company ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(company.companyName)
                    .setMessage(formatLedgerAndVouchers(company))
                    .setPositiveButton("Close", null)
                    .show()
            }
            binding.recyclerOffline.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerOffline.adapter = adapter
        }

        private fun observeViewModel() {
            viewModel.companiesLiveData.observe(viewLifecycleOwner) { companies ->
                adapter.submitList(companies)
                binding.textEmpty.visibility = if (companies.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        private fun formatLedgerAndVouchers(company: OfflineCompany): String {
            val ledger = buildString {
                append("Ledgers:\n")
                for (item in company.ledgerList) append("- ${item.name}: ${item.amount}\n")
            }
            val vouchers = buildString {
                append("\nVouchers:\n")
                for (item in company.voucherList) append("- [${item.date}] ${item.type}: ${item.amount}\n")
            }
            return ledger + vouchers
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }

    class CompanyAdapter(
        val onItemClick: (OfflineCompany) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<OfflineCompany, CompanyAdapter.VH>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<OfflineCompany>() {
            override fun areItemsTheSame(old: OfflineCompany, new: OfflineCompany) = old.id == new.id
            override fun areContentsTheSame(old: OfflineCompany, new: OfflineCompany) = old == new
        }
    ) {
        inner class VH(val binding: ItemCompanyBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemCompanyBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val company = getItem(position)
            holder.binding.textCompanyName.text = company.companyName
            holder.binding.textSerialNumber.text = "Serial No: ${company.serialNumber}"
            holder.binding.textLastSynced.text = "Synced: ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", company.lastSynced)}"
            holder.binding.root.setOnClickListener { onItemClick(company) }
        }
    }
}