package com.cartonapp.dao;

import com.cartonapp.config.DatabaseConnectionFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import com.cartonapp.service.ProductPricingService;

/** Inserts the supplied prototype data once for local development. */
public final class SeedDataDao {
    private final DatabaseConnectionFactory connections;

    public SeedDataDao(DatabaseConnectionFactory connections) { this.connections = connections; }

    public void seed() throws SQLException {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                seedReferences(connection);
                seedCustomers(connection);
                seedProducts(connection);
                seedInvoices(connection);
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
        }
    }

    private void seedReferences(Connection c) throws SQLException {
    }

    private void seedCustomers(Connection c) throws SQLException {
        customer(c,"C-001","الصفوة للأجهزة المنزلية","أجهزة منزلية","0100 234 8671");
        customer(c,"C-002","النيل للصناعات الغذائية","صناعات غذائية","0122 907 4112");
        customer(c,"C-003","أولاد منصور للتجارة","تجارة وتوزيع","0111 640 3098");
        customer(c,"C-004","الدلتا للمنظفات","منظفات","0109 172 6033");
        customer(c,"C-005","رويال للصناعات الدوائية","أدوية","0155 003 8944");
        customer(c,"C-006","المتحدة للزيوت","زيوت","0101 884 6200");
        String[] names={"الشرق للأدوات الصحية","الأمل للأثاث","الوادي للمنتجات الورقية","المصرية للأجهزة","الرواد للتوزيع","الصفوة للمنسوجات","البركة للمواد الغذائية","النور للأدوات المكتبية","المدينة للصناعات","الجزيرة للتجارة","المستقبل للعبوات","النخبة للمنظفات","الوفاء للتوريدات","النجاح للصناعات"};
        String[] industries={"أدوات صحية","أثاث","منتجات ورقية","أجهزة كهربائية","توزيع","منسوجات","مواد غذائية","أدوات مكتبية","صناعات عامة","تجارة","عبوات","منظفات","توريدات","صناعات بلاستيكية"};
        for(int index=0;index<names.length;index++){String code=String.format("C-%03d",index+7);customer(c,code,names[index],industries[index],"0110 "+String.format("%03d %04d",index+100,index+2000));}
    }

    private void customer(Connection c,String code,String name,String industry,String phone) throws SQLException {
        insert(c,"INSERT INTO customers(code,name,industry,phone,address) VALUES (?,?,?,?,?) ON CONFLICT (code) DO NOTHING",code,name,industry,phone,"المنطقة الصناعية الثالثة، مدينة 6 أكتوبر");
    }

    private void seedProducts(Connection c) throws SQLException {
        product(c,"P-1032","C-001","كرتونة خلاط كهربائي",42,31,36,5,2);
        product(c,"P-1028","C-001","كرتونة مروحة سقف",58,28,24,5,2);
        product(c,"P-1011","C-002","كرتونة عبوات صلصة 12",33,24,18,5,2);
        product(c,"P-1007","C-006","كرتونة زيت 6 عبوات",39,27,31,5,2);
        product(c,"P-0998","C-004","كرتونة منظف أرضيات",36,25,29,5,2);
        String[] names={"كرتونة طقم أطباق","كرتونة طاولة صغيرة","كرتونة مناديل","كرتونة غلاية","كرتونة ملابس قطنية","كرتونة مكرونة","كرتونة أقلام","كرتونة لمبات","كرتونة أدوات كهربائية","كرتونة عبوات بلاستيك","كرتونة صابون","كرتونة أكواب","كرتونة قطع غيار","كرتونة مستلزمات مكتبية","كرتونة منتجات زراعية"};
        for(int index=0;index<names.length;index++){String code=String.format("P-%04d",2001+index);String customerCode=String.format("C-%03d",(index%20)+1);product(c,code,customerCode,names[index],30+index,22+index%7,18+index%9,5,index%3+1);}
    }

    private void product(Connection c,String code,String customerCode,String name,int length,int width,int height,int layers,int colors) throws SQLException {
        long customerId=id(c,"SELECT id FROM customers WHERE code=?",customerCode);
        insert(c,"INSERT INTO products(code,customer_id,name,box_type,length_cm,width_cm,height_cm,layer_count,print_colors) VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (code) DO NOTHING",code,customerId,name,"كرتونة شق عادي RSC",bd(length),bd(width),bd(height),layers,colors);
        long productId=id(c,"SELECT id FROM products WHERE code=?",code);
        insert(c,"INSERT INTO product_prices(product_id,effective_date,unit_price) VALUES (?,?,?) ON CONFLICT (product_id,effective_date) DO NOTHING",productId,Date.valueOf(LocalDate.of(2026,5,14)),ProductPricingService.calculate(bd(length),bd(width),bd(height)));
    }

    private void seedInvoices(Connection c) throws SQLException {
        invoice(c,"INV-2026-00048","C-001",LocalDate.of(2026,5,18),"POSTED","68400","20000","مسددة جزئيًا");
        invoice(c,"INV-2026-00047","C-002",LocalDate.of(2026,5,17),"POSTED","39250","0","غير مسددة");
        invoice(c,"INV-2026-00046","C-003",LocalDate.of(2026,5,15),"POSTED","22750","22750","مسددة");
        invoice(c,"INV-2026-00045","C-004",LocalDate.of(2026,5,14),"CANCELLED","54600","0","غير مسددة");
        for(int index=49;index<=64;index++){String number=String.format("INV-2026-%05d",index);String customerCode=String.format("C-%03d",((index-1)%20)+1);String total=BigDecimal.valueOf(12000L+(index-49)*850L).toPlainString();String paid=index%3==0?total:index%3==1?"0":"2500";invoice(c,number,customerCode,LocalDate.of(2026,4,1).plusDays(index-49),"POSTED",total,paid,paid.equals("0")?"غير مسددة":"تحويل بنكي");}
    }

    private void invoice(Connection c,String number,String customerCode,LocalDate date,String status,String total,String paid,String payment) throws SQLException {
        long customerId=id(c,"SELECT id FROM customers WHERE code=?",customerCode);
        insert(c,"INSERT INTO invoices(invoice_number,customer_id,invoice_date,status) VALUES (?,?,?,?) ON CONFLICT (invoice_number) DO NOTHING",number,customerId,Date.valueOf(date),status);
        long invoiceId=id(c,"SELECT id FROM invoices WHERE invoice_number=?",number);
        insert(c,"INSERT INTO invoice_lines(invoice_id,line_number,description,quantity,unit_price) VALUES (?,?,?,?,?) ON CONFLICT (invoice_id,line_number) DO NOTHING",invoiceId,1,"بنود فاتورة العينة",bd("1"),bd(total));
        if (!paid.equals("0")) insert(c,"INSERT INTO receipts(receipt_number,customer_id,invoice_id,receipt_date,amount,payment_method,status) VALUES (?,?,?,?,?,?,?) ON CONFLICT (receipt_number) DO NOTHING","RCPT-"+number.substring(number.length()-5),customerId,invoiceId,Date.valueOf(date.plusDays(1)),bd(paid),"تحويل بنكي","ACTIVE");
    }

    private static long id(Connection c,String sql,Object value) throws SQLException { try(PreparedStatement p=c.prepareStatement(sql)){p.setObject(1,value);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Missing seed reference");return r.getLong(1);}} }
    private static void insert(Connection c,String sql,Object... values) throws SQLException { try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);p.executeUpdate();} }
    private static BigDecimal bd(Object value) { return new BigDecimal(String.valueOf(value)); }
}
