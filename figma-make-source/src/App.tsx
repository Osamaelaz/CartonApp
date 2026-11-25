import { useState, type ReactNode } from "react";

type Tab = "customers" | "products" | "invoices" | "settings";
type View = "list" | "detail";
type Modal = null | "customer" | "product" | "receipt" | "post" | "cancel" | "restore" | "print";

const customers = [
  { code: "C-001", name: "الصفوة للأجهزة المنزلية", industry: "أجهزة منزلية", phone: "0100 234 8671", debt: 184750.5, active: true },
  { code: "C-002", name: "النيل للصناعات الغذائية", industry: "صناعات غذائية", phone: "0122 907 4112", debt: 72400, active: true },
  { code: "C-003", name: "أولاد منصور للتجارة", industry: "تجارة وتوزيع", phone: "0111 640 3098", debt: 0, active: true },
  { code: "C-004", name: "الدلتا للمنظفات", industry: "منظفات", phone: "0109 172 6033", debt: 31800.75, active: false },
  { code: "C-005", name: "رويال للصناعات الدوائية", industry: "أدوية", phone: "0155 003 8944", debt: 12950, active: true },
  { code: "C-006", name: "المتحدة للزيوت", industry: "زيوت", phone: "0101 884 6200", debt: 0, active: true },
];

const products = [
  { code: "P-1032", name: "كرتونة خلاط كهربائي", customer: "الصفوة للأجهزة المنزلية", size: "42 × 31 × 36 سم", price: 42.75 },
  { code: "P-1028", name: "كرتونة مروحة سقف", customer: "الصفوة للأجهزة المنزلية", size: "58 × 28 × 24 سم", price: 51.2 },
  { code: "P-1011", name: "كرتونة عبوات صلصة 12", customer: "النيل للصناعات الغذائية", size: "33 × 24 × 18 سم", price: 18.5 },
  { code: "P-1007", name: "كرتونة زيت 6 عبوات", customer: "المتحدة للزيوت", size: "39 × 27 × 31 سم", price: 27.9 },
  { code: "P-0998", name: "كرتونة منظف أرضيات", customer: "الدلتا للمنظفات", size: "36 × 25 × 29 سم", price: 24.35 },
];

const invoices = [
  { no: "INV-2026-00048", date: "18/05/2026", customer: "الصفوة للأجهزة المنزلية", net: 68400, remaining: 48400, status: "مرحّلة", payment: "مسددة جزئيًا" },
  { no: "INV-2026-00047", date: "17/05/2026", customer: "النيل للصناعات الغذائية", net: 39250, remaining: 39250, status: "مرحّلة", payment: "غير مسددة" },
  { no: "—", date: "17/05/2026", customer: "رويال للصناعات الدوائية", net: 18780, remaining: 18780, status: "مسودة", payment: "غير مسددة" },
  { no: "INV-2026-00046", date: "15/05/2026", customer: "أولاد منصور للتجارة", net: 22750, remaining: 0, status: "مرحّلة", payment: "مسددة" },
  { no: "INV-2026-00045", date: "14/05/2026", customer: "الدلتا للمنظفات", net: 54600, remaining: 0, status: "ملغاة", payment: "غير مسددة" },
];

const money = (value: number) =>
  `${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ج.م`;

function Icon({ name, size = 18 }: { name: string; size?: number }) {
  const paths: Record<string, ReactNode> = {
    users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></>,
    box: <><path d="m21 8-9-5-9 5 9 5 9-5Z"/><path d="m3 8 9 5 9-5M3 8v8l9 5 9-5V8M12 13v8"/></>,
    file: <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h6"/></>,
    gear: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.55V21h-4v-.08A1.7 1.7 0 0 0 8.94 19.4a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.57 15 1.7 1.7 0 0 0 3 14H3v-4h.08A1.7 1.7 0 0 0 4.6 8.94a1.7 1.7 0 0 0-.34-1.88L4.2 7l2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.57 1.7 1.7 0 0 0 10 3V3h4v.08A1.7 1.7 0 0 0 15.06 4.6a1.7 1.7 0 0 0 1.88-.34L17 4.2 19.83 7l-.06.06A1.7 1.7 0 0 0 19.43 9 1.7 1.7 0 0 0 21 10h.08v4H21a1.7 1.7 0 0 0-1.6 1Z"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    download: <><path d="M12 3v12m0 0 4-4m-4 4-4-4M5 20h14"/></>,
    edit: <><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></>,
    print: <><path d="M6 9V2h12v7M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><path d="M6 14h12v8H6z"/></>,
    back: <><path d="M5 12h14M12 5l7 7-7 7"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
    lock: <><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></>,
    more: <><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></>,
    close: <><path d="m6 6 12 12M18 6 6 18"/></>,
    chevron: <path d="m9 18 6-6-6-6"/>,
    alert: <><path d="M10.3 3.7 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.7a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4M12 17h.01"/></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden>{paths[name]}</svg>;
}

function Button({ children, kind = "secondary", icon, onClick, disabled, title }: { children: ReactNode; kind?: "primary" | "secondary" | "danger" | "ghost"; icon?: string; onClick?: () => void; disabled?: boolean; title?: string }) {
  return <button className={`btn btn-${kind}`} onClick={onClick} disabled={disabled} title={title}>{icon && <Icon name={icon} size={16}/>}<span>{children}</span></button>;
}

function Badge({ children, tone = "gray" }: { children: ReactNode; tone?: string }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

function App() {
  const [loggedIn, setLoggedIn] = useState(false);
  const [tab, setTab] = useState<Tab>("customers");
  const [view, setView] = useState<View>("list");
  const [modal, setModal] = useState<Modal>(null);
  const [toast, setToast] = useState("");
  const [search, setSearch] = useState("");
  const [debtorsOnly, setDebtorsOnly] = useState(false);
  const [invoiceDraft, setInvoiceDraft] = useState(false);

  const navigate = (next: Tab) => { setTab(next); setView("list"); };
  const openNewInvoice = () => { setInvoiceDraft(true); setTab("invoices"); setView("detail"); };
  const notify = (text: string) => { setModal(null); setToast(text); window.setTimeout(() => setToast(""), 2800); };

  if (!loggedIn) return <Login onLogin={() => setLoggedIn(true)} />;

  return (
    <div className="desktop-shell" dir="rtl">
      <header className="titlebar">
        <div className="app-mark"><span className="logo-grid"><i/><i/><i/></span><strong>حسابات الكرتون</strong><span className="version">الإصدار 1.0</span></div>
        <div className="window-controls" aria-label="أزرار النافذة"><button>—</button><button>□</button><button className="window-close">×</button></div>
      </header>
      <nav className="tabs">
        <div className="tabs-main">
          <TabButton active={tab === "customers"} icon="users" onClick={() => navigate("customers")}>العملاء</TabButton>
          <TabButton active={tab === "products"} icon="box" onClick={() => navigate("products")}>المنتجات</TabButton>
          <TabButton active={tab === "invoices"} icon="file" onClick={() => navigate("invoices")}>الفواتير</TabButton>
        </div>
        <button className={`settings-tab ${tab === "settings" ? "active" : ""}`} onClick={() => navigate("settings")}><Icon name="gear"/><span>الإعدادات</span></button>
      </nav>
      <main className="workspace">
        {tab === "customers" && (view === "list"
          ? <CustomersList search={search} setSearch={setSearch} debtorsOnly={debtorsOnly} setDebtorsOnly={setDebtorsOnly} open={() => setView("detail")} add={() => setModal("customer")}/>
          : <CustomerPage back={() => setView("list")} modal={setModal} newInvoice={openNewInvoice}/>)}
        {tab === "products" && (view === "list"
          ? <ProductsList open={() => setView("detail")} add={() => setModal("product")}/>
          : <ProductPage back={() => setView("list")} modal={setModal}/>)}
        {tab === "invoices" && (view === "list"
          ? <InvoicesList open={() => { setInvoiceDraft(false); setView("detail"); }} add={openNewInvoice}/>
          : <InvoicePage draft={invoiceDraft} back={() => setView("list")} modal={setModal}/>)}
        {tab === "settings" && <SettingsPage modal={setModal} notify={notify}/>}
      </main>
      {modal && <ModalView type={modal} close={() => setModal(null)} save={notify}/>}
      {toast && <div className="toast"><span>✓</span>{toast}</div>}
    </div>
  );
}

function Login({ onLogin }: { onLogin: () => void }) {
  const [error, setError] = useState(false);
  const [password, setPassword] = useState("");
  const submit = (e: React.FormEvent) => { e.preventDefault(); if (!password) setError(true); else onLogin(); };
  return <div className="login-screen" dir="rtl">
    <div className="login-titlebar"><span>حسابات الكرتون</span><div>—　□　×</div></div>
    <form className="login-card" onSubmit={submit}>
      <span className="login-logo"><span className="logo-grid"><i/><i/><i/></span></span>
      <h1>حسابات الكرتون</h1><p>نظام إدارة حسابات مصنع الكرتون</p>
      <label>كلمة المرور</label>
      <div className={`password-field ${error ? "field-error" : ""}`}><Icon name="lock"/><input autoFocus type="password" value={password} onChange={e => {setPassword(e.target.value); setError(false)}} placeholder="أدخل كلمة المرور"/></div>
      {error && <span className="validation">يرجى إدخال كلمة المرور</span>}
      <Button kind="primary">فتح البرنامج</Button>
      <small>برنامج محلي آمن • نسخة المستخدم الواحد</small>
    </form>
  </div>;
}

function TabButton({ active, icon, children, onClick }: { active: boolean; icon: string; children: ReactNode; onClick: () => void }) {
  return <button className={`tab ${active ? "active" : ""}`} onClick={onClick}><Icon name={icon}/><span>{children}</span></button>;
}

function PageHeader({ eyebrow, title, subtitle, actions, back }: { eyebrow?: string; title: string; subtitle?: ReactNode; actions?: ReactNode; back?: () => void }) {
  return <div className="page-header">
    <div className="heading-wrap">{back && <button className="back-btn" onClick={back}><Icon name="back"/></button>}<div>{eyebrow && <div className="eyebrow">{eyebrow}</div>}<h1>{title}</h1>{subtitle && <div className="subtitle">{subtitle}</div>}</div></div>
    <div className="header-actions">{actions}</div>
  </div>;
}

function SearchBox({ value, onChange, placeholder = "بحث" }: { value: string; onChange: (v: string) => void; placeholder?: string }) {
  return <div className="search"><Icon name="search"/><input value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}/>{value && <button onClick={() => onChange("")}>×</button>}</div>;
}

function Card({ children, className = "" }: { children: ReactNode; className?: string }) { return <section className={`card ${className}`}>{children}</section>; }

function CustomersList({ search, setSearch, debtorsOnly, setDebtorsOnly, open, add }: any) {
  const rows = customers.filter(c => (!debtorsOnly || c.debt > 0) && c.name.includes(search));
  return <>
    <PageHeader eyebrow="إدارة الحسابات" title="العملاء" subtitle={`${customers.length} عملاء مسجلين`} actions={<><Button icon="download">تصدير Excel</Button><Button kind="primary" icon="plus" onClick={add}>عميل جديد</Button></>}/>
    <Card>
      <div className="toolbar"><SearchBox value={search} onChange={setSearch} placeholder="بحث بالاسم أو الكود أو الهاتف"/><label className="check"><input type="checkbox" checked={debtorsOnly} onChange={e => setDebtorsOnly(e.target.checked)}/><span>المدينون فقط</span></label><div className="toolbar-spacer"/><span className="results-count">عرض {rows.length} من {customers.length}</span></div>
      {rows.length ? <div className="table-scroll"><table><thead><tr><Th>الكود</Th><Th>الاسم</Th><Th>الهاتف</Th><Th align="left">المديونية</Th><th>الحالة</th><th></th></tr></thead><tbody>{rows.map(c => <tr key={c.code} onClick={open}><td className="code">{c.code}</td><td><strong>{c.name}</strong><small>{c.industry}</small></td><td dir="ltr" className="phone">{c.phone}</td><td className={`money ${c.debt > 0 ? "debt" : ""}`}>{money(c.debt)}</td><td>{c.active ? <Badge tone="green">نشط</Badge> : <Badge>موقوف</Badge>}</td><td><Icon name="chevron" size={16}/></td></tr>)}</tbody></table></div> : <Empty title="لا توجد نتائج" text="جرّب تغيير كلمات البحث أو إلغاء عوامل التصفية."/>}
      <Pagination count={rows.length}/>
    </Card>
  </>;
}

function ProductsList({ open, add }: any) {
  return <>
    <PageHeader eyebrow="دليل المنتجات" title="المنتجات" subtitle={`${products.length} منتجات مسجلة`} actions={<><Button icon="file">نسخ منتج</Button><Button kind="primary" icon="plus" onClick={add}>منتج جديد</Button></>}/>
    <Card><div className="toolbar"><SearchBox value="" onChange={() => {}} placeholder="بحث بالكود أو اسم المنتج"/><select><option>كل العملاء</option><option>الصفوة للأجهزة المنزلية</option><option>النيل للصناعات الغذائية</option></select><div className="toolbar-spacer"/><span className="results-count">عرض {products.length} منتجات</span></div>
      <div className="table-scroll"><table><thead><tr><Th>الكود</Th><Th>الاسم</Th><Th>العميل</Th><Th>المقاس</Th><Th align="left">سعر الكرتونة</Th><th></th></tr></thead><tbody>{products.map(p => <tr key={p.code} onClick={open}><td className="code">{p.code}</td><td><strong>{p.name}</strong></td><td>{p.customer}</td><td dir="ltr" className="size">{p.size}</td><td className="money">{money(p.price)}</td><td><Icon name="chevron" size={16}/></td></tr>)}</tbody></table></div><Pagination count={products.length}/></Card>
  </>;
}

function InvoicesList({ open, add }: any) {
  return <>
    <PageHeader eyebrow="المبيعات والتحصيل" title="الفواتير" subtitle="جميع الفواتير والمسودات" actions={<Button kind="primary" icon="plus" onClick={add}>فاتورة جديدة</Button>}/>
    <div className="metric-row compact"><Metric label="إجمالي مرحّل هذا الشهر" value={money(527340)} tone="blue"/><Metric label="المحصّل" value={money(342589.5)} tone="green"/><Metric label="المتبقي" value={money(184750.5)} tone="orange"/></div>
    <Card><div className="toolbar"><SearchBox value="" onChange={() => {}} placeholder="بحث برقم الفاتورة أو العميل"/><select><option>كل الحالات</option><option>مسودة</option><option>مرحّلة</option><option>ملغاة</option></select><div className="toolbar-spacer"/><span className="results-count">5 فواتير</span></div>
      <div className="table-scroll"><table><thead><tr><Th>رقم الفاتورة</Th><Th>التاريخ</Th><Th>العميل</Th><Th align="left">الصافي</Th><Th align="left">المتبقي</Th><th>الحالة</th><th>حالة السداد</th><th></th></tr></thead><tbody>{invoices.map((i, n) => <tr key={n} onClick={open}><td className="code">{i.no}</td><td>{i.date}</td><td><strong>{i.customer}</strong></td><td className="money">{money(i.net)}</td><td className="money">{money(i.remaining)}</td><td><Status value={i.status}/></td><td><Status value={i.payment}/></td><td><Icon name="chevron" size={16}/></td></tr>)}</tbody></table></div><Pagination count={5}/></Card>
  </>;
}

function CustomerPage({ back, modal, newInvoice }: { back: () => void; modal: (m: Modal) => void; newInvoice: () => void }) {
  return <>
    <PageHeader eyebrow="العملاء / C-001" title="الصفوة للأجهزة المنزلية" subtitle={<><Badge tone="green">نشط</Badge><span>أجهزة منزلية</span></>} back={back} actions={<><Button icon="edit" onClick={() => modal("customer")}>تعديل</Button><Button onClick={() => modal("print")} icon="print">طباعة كشف حساب</Button><Button kind="primary" icon="plus" onClick={newInvoice}>فاتورة جديدة</Button><Button kind="danger">إيقاف</Button></>}/>
    <Card className="info-card"><div className="info-grid"><Info label="الكود" value="C-001"/><Info label="الهاتف" value="0100 234 8671"/><Info label="العنوان" value="المنطقة الصناعية الثالثة، مدينة 6 أكتوبر"/><Info label="الرقم الضريبي" value="248-761-903"/><Info label="أيام السداد" value="30 يوم"/><Info label="معفي من الضريبة" value="لا"/></div></Card>
    <div className="metric-row"><Metric label="إجمالي الفواتير" value={money(527340)}/><Metric label="المحصّل" value={money(342589.5)} tone="green"/><Metric label="المديونية" value={money(184750.5)} tone="orange"/></div>
    <div className="subtabs"><button className="active">كشف الحساب</button><button>منتجات العميل <span>2</span></button><button>فواتير العميل <span>14</span></button></div>
    <Card>
      <div className="card-title"><div><h2>كشف الحساب</h2><p>حركة الفواتير والتحصيلات — الصفوف السابقة لا يتم تعديلها</p></div><div className="date-range"><Icon name="calendar"/><span>01/05/2026</span><b>إلى</b><span>31/05/2026</span></div><Button kind="primary" icon="plus" onClick={() => modal("receipt")}>إضافة تحصيل</Button></div>
      <div className="table-scroll statement"><table><thead><tr><Th>التاريخ</Th><Th>البيان</Th><Th>رقم الفاتورة</Th><Th align="left">مدين</Th><Th align="left">دائن</Th><Th align="left">الرصيد</Th></tr></thead><tbody>
        <Statement date="18/05/2026" desc="فاتورة مبيعات مرحّلة" inv="INV-2026-00048" debit={68400} balance={184750.5}/>
        <Statement date="16/05/2026" desc="تحصيل — تحويل بنكي" inv="INV-2026-00042" credit={20000} balance={116350.5}/>
        <Statement date="12/05/2026" desc="فاتورة مبيعات مرحّلة" inv="INV-2026-00042" debit={48500} balance={136350.5}/>
        <Statement date="08/05/2026" desc="إلغاء تحصيل RCPT-00184" inv="INV-2026-00036" debit={10000} balance={87850.5}/>
        <Statement date="05/05/2026" desc="تحصيل — شيك" inv="INV-2026-00036" credit={35000} balance={77850.5}/>
        <Statement date="02/05/2026" desc="عكس فاتورة ملغاة" inv="INV-2026-00031" credit={12500} balance={112850.5}/>
      </tbody><tfoot><tr><td colSpan={3}>إجمالي الفترة</td><td>{money(126900)}</td><td>{money(67500)}</td><td>{money(184750.5)}</td></tr></tfoot></table></div>
      <Pagination count={28}/>
    </Card>
  </>;
}

function ProductPage({ back, modal }: { back: () => void; modal: (m: Modal) => void }) {
  return <>
    <PageHeader eyebrow="المنتجات / P-1032" title="كرتونة خلاط كهربائي" subtitle="الصفوة للأجهزة المنزلية" back={back} actions={<><Button icon="edit" onClick={() => modal("product")}>تعديل</Button><Button kind="primary">إعادة تسعير</Button></>}/>
    <div className="product-layout">
      <div className="stack">
        <Card><SectionTitle title="مواصفات الكرتونة"/><div className="spec-grid"><Info label="نوع العلبة" value="كرتونة شق عادي RSC"/><Info label="المقاس" value="42 × 31 × 36 سم"/><Info label="عدد الطبقات" value="5 طبقات"/><Info label="نوع الفلوت" value="BC Flute"/><Info label="لسان اللصق" value="4 سم"/><Info label="التشذيب" value="2 سم"/><Info label="ألوان الطباعة" value="2 لون"/><Info label="مساحة الفرخ (محسوبة)" value="1.284 م²" highlight/><Info label="وزن الكرتونة (محسوب)" value="0.862 كجم" highlight/></div></Card>
        <Card><SectionTitle title="طبقات الورق" action={<Button kind="ghost" icon="edit">تعديل</Button>}/><table className="mini-table"><thead><tr><th>#</th><th>نوع الطبقة</th><th>نوع الورق</th><th>GSM</th><th>السعر / كجم</th></tr></thead><tbody><tr><td>1</td><td><Badge tone="blue">وش</Badge></td><td>كرافت مستورد</td><td>180</td><td>{money(42)}</td></tr><tr><td>2</td><td><Badge tone="orange">فلوت</Badge></td><td>فلوت نصف كيميائي</td><td>150</td><td>{money(31.5)}</td></tr><tr><td>3</td><td><Badge>بطانة</Badge></td><td>تست لاينر</td><td>125</td><td>{money(27.75)}</td></tr></tbody></table></Card>
        <Card><SectionTitle title="مكونات التسعير"/><table className="mini-table"><thead><tr><th>المكوّن</th><th>طريقة الحساب</th><th>المعدل</th><th>التكلفة</th></tr></thead><tbody><tr><td>لصق</td><td>ثابت لكل كرتونة</td><td>1.25</td><td>{money(1.25)}</td></tr><tr><td>طباعة — لونان</td><td>لكل متر مربع</td><td>2.10</td><td>{money(2.7)}</td></tr><tr><td>تشغيل الكريز</td><td>لكل كرتونة</td><td>0.85</td><td>{money(.85)}</td></tr><tr><td>نقل</td><td>لكل كرتونة</td><td>0.60</td><td>{money(.6)}</td></tr></tbody></table></Card>
      </div>
      <div className="stack">
        <Card className="price-card"><div className="price-head"><span>سعر الكرتونة</span><strong>{money(42.75)}</strong><small>آخر تحديث: 14/05/2026</small></div><div className="breakdown"><p><span>تكلفة الورق</span><b>{money(24.82)}</b></p><p><span>مكونات التشغيل</span><b>{money(5.40)}</b></p><p><span>هالك 7.00%</span><b>{money(2.12)}</b></p><p><span>مصروفات غير مباشرة 8.00%</span><b>{money(2.59)}</b></p><p><span>هامش ربح 22.00%</span><b>{money(7.82)}</b></p><p className="total"><span>تكلفة الوحدة</span><b>{money(34.93)}</b></p><p className="grand"><span>سعر الوحدة</span><b>{money(42.75)}</b></p></div></Card>
        <Card><SectionTitle title="تاريخ الأسعار"/><div className="history"><div><span>14/05/2026</span><p><del>{money(40.5)}</del><Icon name="back" size={14}/><strong>{money(42.75)}</strong></p></div><div><span>02/03/2026</span><p><del>{money(38)}</del><Icon name="back" size={14}/><strong>{money(40.5)}</strong></p></div><div><span>15/01/2026</span><p><del>{money(36.25)}</del><Icon name="back" size={14}/><strong>{money(38)}</strong></p></div></div></Card>
      </div>
    </div>
  </>;
}

function InvoicePage({ back, modal, draft }: { back: () => void; modal: (m: Modal) => void; draft: boolean }) {
  return <>
    <PageHeader eyebrow={draft ? "الفواتير / مسودة جديدة" : "الفواتير / INV-2026-00048"} title={draft ? "فاتورة جديدة" : "فاتورة INV-2026-00048"} subtitle={draft ? <><Badge>مسودة</Badge><span>لا يصدر رقم الفاتورة إلا بعد الترحيل</span></> : <><Badge tone="blue">مرحّلة</Badge><Badge tone="orange">مسددة جزئيًا</Badge></>} back={back} actions={draft ? <><Button icon="edit">حفظ</Button><Button kind="primary" onClick={() => modal("post")}>ترحيل</Button><Button kind="danger">حذف</Button></> : <><Button icon="print" onClick={() => modal("print")}>طباعة / تصدير PDF</Button><Button onClick={() => modal("receipt")}>إضافة تحصيل</Button><Button kind="danger" disabled title="ألغِ التحصيلات النشطة أولًا">إلغاء الفاتورة</Button></>}/>
    <Card className="invoice-head"><div className="info-grid">{draft ? <><Info label="العميل" value="الصفوة للأجهزة المنزلية"/><Info label="رقم الفاتورة" value="يُنشأ عند الترحيل"/><Info label="التاريخ" value="20/05/2026"/><Info label="تاريخ الاستحقاق" value="19/06/2026 (30 يوم)"/><Info label="نسبة الضريبة" value="14.00%"/><Info label="ملاحظات" value="أمر شراء 8910"/></> : <><Info label="العميل" value="الصفوة للأجهزة المنزلية"/><Info label="التاريخ" value="18/05/2026"/><Info label="تاريخ الاستحقاق" value="17/06/2026"/><Info label="نسبة الضريبة" value="14.00%"/><Info label="ملاحظات" value="التسليم بمخزن العميل — أمر شراء 8821"/></>}</div></Card>
    <Card><SectionTitle title="بنود الفاتورة"/><table className="invoice-lines"><thead><tr><th>م</th><th>المنتج</th><th>الكمية</th><th>سعر الوحدة</th><th>الإجمالي</th></tr></thead><tbody><tr><td>1</td><td><strong>كرتونة خلاط كهربائي</strong><small>P-1032</small></td><td>1,200</td><td>{money(42.75)}</td><td>{money(51300)}</td></tr><tr><td>2</td><td><strong>كرتونة مروحة سقف</strong><small>P-1028</small></td><td>300</td><td>{money(51.2)}</td><td>{money(15360)}</td></tr></tbody></table><div className="totals"><p><span>الإجمالي قبل الخصم</span><b>{money(66660)}</b></p><p><span>الخصم</span><b>{money(666.6)}</b></p><p><span>الضريبة (14.00%)</span><b>{money(9239.08)}</b></p><p className="net"><span>الصافي</span><b>{money(75232.48)}</b></p><p><span>المحصّل</span><b className="green-text">{money(20000)}</b></p><p><span>المتبقي</span><b className="orange-text">{money(55232.48)}</b></p></div></Card>
    {!draft && <Card><SectionTitle title="التحصيلات" action={<Button kind="ghost" icon="plus" onClick={() => modal("receipt")}>إضافة تحصيل</Button>}/><table className="mini-table"><thead><tr><th>رقم التحصيل</th><th>التاريخ</th><th>طريقة الدفع</th><th>المرجع</th><th>المبلغ</th><th>الحالة</th><th></th></tr></thead><tbody><tr><td className="code">RCPT-00192</td><td>19/05/2026</td><td>تحويل بنكي</td><td>TRX-590214</td><td>{money(20000)}</td><td><Badge tone="green">نشط</Badge></td><td><Button kind="ghost" onClick={() => modal("cancel")}>إلغاء التحصيل</Button></td></tr></tbody></table></Card>}
  </>;
}

function SettingsPage({ modal, notify }: { modal: (m: Modal) => void; notify: (s: string) => void }) {
  return <>
    <PageHeader eyebrow="تخصيص البرنامج" title="الإعدادات" subtitle="بيانات التسعير والنسخ الاحتياطي"/>
    <div className="settings-layout">
      <aside className="settings-nav"><button className="active">إعدادات التسعير</button><button>أنواع الورق</button><button>أنواع الفلوت</button><button>مكونات التسعير</button><button>الأمان وكلمة المرور</button><button>النسخة الاحتياطية</button></aside>
      <div className="stack">
        <Card><SectionTitle title="الإعدادات الافتراضية" subtitle="تُستخدم تلقائيًا عند إنشاء منتج جديد"/><div className="form-grid"><Field label="لسان اللصق الافتراضي" value="4" suffix="سم"/><Field label="التشذيب الافتراضي" value="2" suffix="سم"/><Field label="نسبة الهالك" value="7.00" suffix="%"/><Field label="نسبة الضريبة" value="14.00" suffix="%"/></div><div className="card-footer"><Button kind="primary" onClick={() => notify("تم حفظ الإعدادات بنجاح")}>حفظ</Button></div></Card>
        <Card><SectionTitle title="أنواع الورق" action={<Button icon="plus">إضافة</Button>}/><table className="mini-table"><thead><tr><th>نوع الورق</th><th>GSM الافتراضي</th><th>السعر لكل كجم</th><th></th></tr></thead><tbody><tr><td>كرافت مستورد</td><td>180</td><td>{money(42)}</td><td><Button kind="ghost" icon="edit">تعديل</Button></td></tr><tr><td>فلوت نصف كيميائي</td><td>150</td><td>{money(31.5)}</td><td><Button kind="ghost" icon="edit">تعديل</Button></td></tr><tr><td>تست لاينر</td><td>125</td><td>{money(27.75)}</td><td><Button kind="ghost" icon="edit">تعديل</Button></td></tr></tbody></table></Card>
        <Card><SectionTitle title="أنواع الفلوت" action={<Button icon="plus">إضافة</Button>}/><table className="mini-table"><thead><tr><th>النوع</th><th>معامل التموج</th><th></th></tr></thead><tbody><tr><td>B Flute</td><td>1.35</td><td><Button kind="ghost" icon="edit">تعديل</Button></td></tr><tr><td>C Flute</td><td>1.43</td><td><Button kind="ghost" icon="edit">تعديل</Button></td></tr></tbody></table></Card>
        <Card><SectionTitle title="الأمان والنسخة الاحتياطية"/><div className="backup-grid"><div><h3>كلمة المرور</h3><p>غيّر كلمة مرور فتح البرنامج بصورة دورية.</p><Button icon="lock">تغيير الباسورد</Button></div><div className="backup-box"><div className="backup-icon"><Icon name="download" size={24}/></div><div><h3>نسخة احتياطية</h3><p>المجلد: <span dir="ltr">E:\Carton Accounts\Backup</span></p><small><Badge tone="green">ناجحة</Badge> آخر نسخة: اليوم، 08:42 ص</small><em>تعمل نسخة احتياطية يومية تلقائيًا عند فتح البرنامج.</em></div><div className="backup-actions"><Button kind="primary" onClick={() => notify("تم إنشاء النسخة الاحتياطية بنجاح")}>نسخ احتياطي الآن</Button><Button onClick={() => modal("restore")}>استعادة نسخة</Button></div></div></div></Card>
      </div>
    </div>
  </>;
}

function ModalView({ type, close, save }: { type: Exclude<Modal, null>; close: () => void; save: (s: string) => void }) {
  const title = type === "customer" ? "عميل جديد" : type === "product" ? "منتج جديد" : type === "receipt" ? "إضافة تحصيل" : type === "post" ? "ترحيل الفاتورة" : type === "cancel" ? "إلغاء التحصيل" : type === "restore" ? "استعادة نسخة احتياطية" : "معاينة الطباعة";
  if (type === "print") return <div className="modal-backdrop"><div className="modal print-modal"><ModalHead title={title} close={close}/><div className="print-toolbar"><span>فاتورة A4 — عربي RTL</span><div><Button onClick={close}>تراجع</Button><Button kind="primary" icon="print" onClick={() => save("تم إرسال المستند إلى الطابعة")}>طباعة / تصدير PDF</Button></div></div><div className="paper"><div className="paper-head"><div><h2>مصنع الكرتون الحديث</h2><p>المنطقة الصناعية، 6 أكتوبر — مصر</p><p>الرقم الضريبي: 475-390-218</p></div><div><h1>فاتورة مبيعات</h1><strong dir="ltr">INV-2026-00048</strong></div></div><hr/><div className="paper-info"><p><span>العميل</span><b>الصفوة للأجهزة المنزلية</b></p><p><span>التاريخ</span><b>18/05/2026</b></p><p><span>تاريخ الاستحقاق</span><b>17/06/2026</b></p></div><table><thead><tr><th>م</th><th>البيان</th><th>الكمية</th><th>السعر</th><th>الإجمالي</th></tr></thead><tbody><tr><td>1</td><td>كرتونة خلاط كهربائي</td><td>1,200</td><td>42.75</td><td>51,300.00</td></tr><tr><td>2</td><td>كرتونة مروحة سقف</td><td>300</td><td>51.20</td><td>15,360.00</td></tr></tbody></table><div className="paper-total"><span>الصافي شامل الضريبة</span><b>{money(75232.48)}</b></div><footer>شكرًا لتعاملكم معنا</footer></div></div></div>;
  if (["post", "cancel", "restore"].includes(type)) return <div className="modal-backdrop"><div className="modal confirm-modal"><ModalHead title={title} close={close}/><div className="confirm-body"><span className={`confirm-icon ${type === "restore" ? "warn" : "danger"}`}><Icon name={type === "restore" ? "download" : "alert"} size={28}/></span><h2>{type === "cancel" ? "هل تريد إلغاء هذا التحصيل؟" : type === "restore" ? "هل تريد استعادة هذه النسخة؟" : "هل تريد ترحيل الفاتورة؟"}</h2><p>{type === "cancel" ? "سيُضاف قيد عكسي مدين إلى كشف حساب العميل. لا يمكن حذف الحركة القديمة." : type === "restore" ? "سيتم استبدال البيانات الحالية بالكامل. يُنصح بإنشاء نسخة احتياطية أولًا." : "سيتم إنشاء رقم تسلسلي وقفل بنود الفاتورة نهائيًا."}</p></div><div className="modal-footer"><Button onClick={close}>تراجع</Button><Button kind={type === "restore" ? "primary" : "danger"} onClick={() => save(type === "restore" ? "تمت استعادة النسخة بنجاح" : "تم تنفيذ العملية بنجاح")}>{type === "restore" ? "استعادة" : type === "cancel" ? "إلغاء التحصيل" : "ترحيل"}</Button></div></div></div>;
  return <div className="modal-backdrop"><div className={`modal form-modal ${type === "product" ? "wide" : ""}`}><ModalHead title={title} close={close}/><div className="modal-body">
    {type === "customer" && <><div className="form-grid two"><Field label="الكود" value="يُنشأ تلقائيًا" disabled/><Field label="الاسم *" placeholder="اسم العميل أو الشركة"/><Field label="النشاط" placeholder="مثال: أجهزة منزلية"/><Field label="الهاتف" placeholder="01xx xxx xxxx"/><Field label="العنوان" placeholder="عنوان العميل" full/><Field label="الرقم الضريبي" placeholder="000-000-000"/><Field label="أيام السداد" value="30" suffix="يوم"/><label className="toggle-row"><input type="checkbox"/><span>معفي من الضريبة</span></label></div><div className="validation-box">الحقول المعلّمة بـ * مطلوبة للحفظ</div></>}
    {type === "product" && <><div className="form-section"><h3>البيانات الأساسية</h3><div className="form-grid two"><SelectField label="العميل *" options={["اختر العميل", "الصفوة للأجهزة المنزلية", "النيل للصناعات الغذائية"]}/><Field label="الاسم *" placeholder="اسم المنتج"/><Field label="نوع العلبة" value="كرتونة شق عادي RSC"/><SelectField label="نوع الفلوت" options={["B Flute", "C Flute", "BC Flute"]}/></div></div><div className="form-section"><h3>الأبعاد والمواصفات</h3><div className="form-grid four"><Field label="الطول" value="42" suffix="سم"/><Field label="العرض" value="31" suffix="سم"/><Field label="الارتفاع" value="36" suffix="سم"/><Field label="عدد الطبقات" value="5"/><Field label="لسان اللصق" value="4" suffix="سم"/><Field label="التشذيب" value="2" suffix="سم"/><Field label="ألوان الطباعة" value="2"/></div></div><div className="form-section"><SectionTitle title="طبقات الورق" action={<Button kind="ghost" icon="plus">إضافة طبقة</Button>}/><div className="empty-inline">أضف طبقات الورق لحساب التكلفة والوزن تلقائيًا.</div></div></>}
    {type === "receipt" && <><div className="receipt-summary"><span>الفاتورة</span><strong dir="ltr">INV-2026-00048</strong><p>المتبقي: <b>{money(55232.48)}</b></p></div><div className="form-grid two"><Field label="المبلغ *" value="20000.00" suffix="ج.م"/><Field label="التاريخ *" value="19/05/2026"/><SelectField label="طريقة الدفع *" options={["نقدي", "شيك", "تحويل بنكي"]}/><Field label="المرجع" placeholder="رقم المرجع أو الشيك"/><Field label="تاريخ استحقاق الشيك" placeholder="يوم / شهر / سنة"/></div><p className="form-hint">يجب أن يكون المبلغ أكبر من 0.00 ج.م وألا يتجاوز المتبقي.</p></>}
  </div><div className="modal-footer"><Button onClick={close}>تراجع</Button><Button kind="primary" onClick={() => save(type === "receipt" ? "تم تسجيل التحصيل بنجاح" : "تم حفظ البيانات بنجاح")}>حفظ</Button></div></div></div>;
}

function ModalHead({ title, close }: { title: string; close: () => void }) { return <div className="modal-head"><div><span className="modal-icon"><Icon name="file"/></span><h2>{title}</h2></div><button onClick={close}><Icon name="close"/></button></div>; }
function Th({ children, align }: { children: ReactNode; align?: string }) { return <th className={align === "left" ? "align-left" : ""}>{children}<span className="sort">↕</span></th>; }
function Status({ value }: { value: string }) { const tone = value === "مرحّلة" ? "blue" : value === "مسودة" ? "gray" : value === "ملغاة" ? "red" : value === "مسددة" ? "green" : value === "مسددة جزئيًا" ? "orange" : "red"; return <Badge tone={tone}>{value}</Badge>; }
function Pagination({ count }: { count: number }) { return <div className="pagination"><span>إجمالي السجلات: {count}</span><div><button disabled>‹</button><button className="current">1</button><button>2</button><button>3</button><button>›</button></div><span>50 سجل في الصفحة</span></div>; }
function Empty({ title, text }: { title: string; text: string }) { return <div className="empty"><span><Icon name="search" size={26}/></span><h3>{title}</h3><p>{text}</p></div>; }
function Info({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) { return <div className={`info ${highlight ? "highlight" : ""}`}><span>{label}</span><strong>{value}</strong></div>; }
function Metric({ label, value, tone = "", note }: { label: string; value: string; tone?: string; note?: ReactNode }) { return <Card className={`metric metric-${tone}`}><span>{label}</span><strong>{value}</strong>{note && <div>{note}</div>}</Card>; }
function SectionTitle({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) { return <div className="section-title"><div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>{action}</div>; }
function Statement({ date, desc, inv, debit, credit, balance }: { date: string; desc: string; inv: string; debit?: number; credit?: number; balance: number }) { return <tr><td>{date}</td><td><strong>{desc}</strong></td><td className="code">{inv}</td><td className="money">{debit ? money(debit) : "—"}</td><td className="money green-text">{credit ? money(credit) : "—"}</td><td className="money"><strong>{money(balance)}</strong></td></tr>; }
function Field({ label, value, placeholder, suffix, disabled, full }: { label: string; value?: string; placeholder?: string; suffix?: string; disabled?: boolean; full?: boolean }) { return <label className={`field ${full ? "full" : ""}`}><span>{label}</span><div><input defaultValue={value} placeholder={placeholder} disabled={disabled}/>{suffix && <em>{suffix}</em>}</div></label>; }
function SelectField({ label, options }: { label: string; options: string[] }) { return <label className="field"><span>{label}</span><select>{options.map(o => <option key={o}>{o}</option>)}</select></label>; }

export default App;
