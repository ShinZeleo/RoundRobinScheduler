import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;

public class RoundRobinSchedulerApp extends JFrame {

    // ===== Model data. Tidak berubah =====
    static class Proc {
        String pid;
        int at, bt, rt, ct = -1;
        Proc(String pid, int at, int bt){ this.pid = pid; this.at = at; this.bt = bt; this.rt = bt; }
    }
    static class Seg { String pid; int start, end; Seg(String pid,int s,int e){ this.pid=pid; this.start=s; this.end=e; } }

    // ===== Komponen data =====
    private final DefaultTableModel inputModel  = new DefaultTableModel(new Object[]{"Proses","Arrival","Burst"},0);
    private final JTable inputTable = new StripedTable(inputModel);

    private final DefaultTableModel resultModel =
            new DefaultTableModel(new Object[]{"Proses","AT","BT","Finish","WT","TT"}, 0);

    private final JTable resultTable = new StripedTable(resultModel);

    private final JTextArea ganttText = new JTextArea(8, 60);

    private final JComboBox<String> algoCombo = new JComboBox<>(new String[]{
            "Round Robin Standar",
            "RR Pengembangan. Quantum gandakan. Pilih sisa terpendek"
    });
    private final JSpinner quantumSpinner = new JSpinner(new SpinnerNumberModel(4,1,1000,1));
    private final JComboBox<String> datasetCombo = new JComboBox<>(new String[]{
            "Tabel 1. BT seimbang",
            "Tabel 2. BT bervariasi",
            "Tabel 3. AT dan BT berbeda",
            "Tabel 4. Proses pendek datang terlambat",
            "Tabel 5. AT acak, proses dominan"
    });

    public RoundRobinSchedulerApp(){
        super("Round Robin Scheduler. Fokus Data");

        applyLookAndFeel();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8,8));
        setSize(1100, 800);
        setLocationRelativeTo(null);

        // ===== Panel kontrol atas. Rapi dan ringkas
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBorder(new EmptyBorder(8,10,0,10));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton runBtn   = new JButton("Hitung");
        JButton addRow   = new JButton("Tambah");
        JButton delRow   = new JButton("Hapus");
        JButton clearBtn = new JButton("Bersihkan");

        row1.add(new JLabel("Algoritma"));
        row1.add(algoCombo);
        row1.add(new JLabel("Quantum"));
        quantumSpinner.setPreferredSize(new Dimension(72, quantumSpinner.getPreferredSize().height));
        row1.add(quantumSpinner);
        row1.add(runBtn);
        row1.add(addRow);
        row1.add(delRow);
        row1.add(clearBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row2.add(new JLabel("Pilihan Dataset"));
        datasetCombo.setEditable(false);
        row2.add(datasetCombo);

        north.add(row1);
        north.add(row2);
        add(north, BorderLayout.NORTH);

        // ===== Tabel input dan hasil. Scrollbar default
        styleTables();

        JScrollPane spInput = new JScrollPane(inputTable);
        spInput.setBorder(makeTitle("Input Proses"));

        JScrollPane spRes = new JScrollPane(resultTable);
        spRes.setBorder(makeTitle("Hasil WT, TAT, CT"));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(6,10,6,10));
        center.add(spInput);
        center.add(Box.createVerticalStrut(8));
        center.add(spRes);
        add(center, BorderLayout.CENTER);

        // ===== Urutan Gantt. Scrollbar default
        ganttText.setEditable(false);
        ganttText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        ganttText.setBorder(new EmptyBorder(8,10,8,10));
        JScrollPane spGantt = new JScrollPane(ganttText);
        spGantt.setBorder(makeTitle("Urutan Gantt. Format [start..end] pid"));
        add(spGantt, BorderLayout.SOUTH);

        // ===== Actions. Logika tidak berubah
        addRow.addActionListener(e -> inputModel.addRow(new Object[]{"p"+(inputModel.getRowCount()+1),0,1}));
        delRow.addActionListener(e -> { int r=inputTable.getSelectedRow(); if(r>=0) inputModel.removeRow(r); });
        clearBtn.addActionListener(e -> { inputModel.setRowCount(0); resultModel.setRowCount(0); ganttText.setText(""); });
        runBtn.addActionListener(e -> runScheduling());

        datasetCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (inputTable.isEditing()) inputTable.getCellEditor().stopCellEditing();
                switch (datasetCombo.getSelectedIndex()){
                    case 0 -> loadDataset1();
                    case 1 -> loadDataset2();
                    case 2 -> loadDataset3();
                    case 3 -> loadDataset4();
                    case 4 -> loadDataset5();
                }
            }
        });

        // Isi awal
        datasetCombo.setSelectedIndex(0);
        loadDataset1();
    }

    // ======= Tampilan
    private void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
        UIManager.put("Label.font",  new Font("Segoe UI", Font.BOLD, 14));
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Table.font", new Font("Segoe UI", Font.BOLD, 18));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 14));
        UIManager.put("TextArea.font", new Font("Consolas", Font.PLAIN, 18));
    }

    private void styleTables() {
        inputTable.setRowHeight(28);
        resultTable.setRowHeight(28);
        inputTable.setFillsViewportHeight(true);
        resultTable.setFillsViewportHeight(true);
        inputTable.setShowGrid(true);
        resultTable.setShowGrid(true);

        // Header tebal dan rata tengah
        JTableHeader h1 = inputTable.getTableHeader();
        JTableHeader h2 = resultTable.getTableHeader();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) h1.getDefaultRenderer();
        hr.setHorizontalAlignment(SwingConstants.CENTER);
        h1.setDefaultRenderer(hr);
        h2.setDefaultRenderer(hr);

        // Semua isi sel rata tengah
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < inputTable.getColumnCount(); i++) {
            inputTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        // Lebar kolom
        inputTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        inputTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        inputTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        // Cegah drag urutan kolom
        inputTable.getTableHeader().setReorderingAllowed(false);
        resultTable.getTableHeader().setReorderingAllowed(false);
    }


    private TitledBorder makeTitle(String title) {
        TitledBorder tb = new TitledBorder(new LineBorder(new Color(210,210,210)),
                title, TitledBorder.LEFT, TitledBorder.TOP);
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        return tb;
    }

    // ======= Logika penjadwalan. Tidak diubah =======
    /** Menjalankan seluruh alur:
     *  1. Baca input dari tabel.
     *  2. Salin proses agar input tidak berubah.
     *  3. Jalankan algoritma terpilih.
     *  4. Hitung waktu CT, WT, TAT.
     *  5. Tampilkan hasil dan Gantt.
     */
    private void runScheduling(){
        List<Proc> procs = readInput();
        if(procs.isEmpty()){
            JOptionPane.showMessageDialog(this,"Isi data dulu");
            return;
        }
        int q = (Integer) quantumSpinner.getValue();

        // Salin objek proses. Tujuannya agar field rt dan ct yang berubah
        // tidak mengubah data asli yang tertera di tabel input.
        List<Proc> work = new ArrayList<>();
        for(Proc p: procs) work.add(new Proc(p.pid,p.at,p.bt));

        // Pilih algoritma sesuai combobox
        List<Seg> segs = (algoCombo.getSelectedIndex()==0)
                ? rrStandard(work,q)
                : rrDeveloped(work,q);

        // Turunan metrik waktu
        computeTimes(work,segs);
        fillResultTable(work);
        showGantt(segs);
    }

    /** Membaca baris dari tabel input. Validasi angka. Urutkan berdasarkan AT dan pid. */
    private List<Proc> readInput(){
        List<Proc> list=new ArrayList<>();
        for(int i=0;i<inputModel.getRowCount();i++){
            Object pidObj=inputModel.getValueAt(i,0);
            Object atObj =inputModel.getValueAt(i,1);
            Object btObj =inputModel.getValueAt(i,2);
            if(pidObj==null||atObj==null||btObj==null) continue;
            try{
                String pid=pidObj.toString().trim();
                int at=Integer.parseInt(atObj.toString().trim());
                int bt=Integer.parseInt(btObj.toString().trim());
                if(bt<=0){
                    JOptionPane.showMessageDialog(this,"Burst harus positif di baris "+(i+1));
                    return Collections.emptyList();
                }
                list.add(new Proc(pid,at,bt));
            }catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(this,"Angka tidak valid di baris "+(i+1));
                return Collections.emptyList();
            }
        }
        // Urut supaya simulasi dimulai dari proses yang datang paling awal
        list.sort(Comparator.<Proc>comparingInt(p->p.at).thenComparing(p->p.pid));
        return list;
    }

    /** AT terkecil di antara proses yang ada. Dipakai sebagai waktu mulai simulasi. */
    private int minArrival(List<Proc> procs){
        int m=Integer.MAX_VALUE;
        for(Proc p:procs) m=Math.min(m,p.at);
        return m==Integer.MAX_VALUE?0:m;
    }

    /** Mencari waktu kedatangan berikutnya setelah t untuk proses yang belum selesai.
     *  Dipakai saat ready queue kosong agar waktu melompat ke momen kedatangan berikutnya.
     */
    private OptionalInt nextArrivalAfter(List<Proc> procs,int t){
        int best=Integer.MAX_VALUE;
        for(Proc p:procs) if(p.rt>0 && p.at>t) best=Math.min(best,p.at);
        return best==Integer.MAX_VALUE?OptionalInt.empty():OptionalInt.of(best);
    }

    /** Round Robin standar dengan satu ready queue.
     *  Setiap proses dapat jatah waktu sebesar quantum.
     *  Jika belum selesai maka ditaruh kembali ke belakang queue.
     */
    private List<Seg> rrStandard(List<Proc> procs,int quantum){
        List<Seg> segs=new ArrayList<>();

        int n = procs.size();
        int time = minArrival(procs);   // waktu simulasi saat ini
        int completed = 0;              // jumlah proses yang sudah selesai

        ArrayDeque<Proc> rq = new ArrayDeque<>(); // ready queue
        boolean[] seen = new boolean[n];          // penanda proses sudah pernah dimasukkan ke queue

        while(true){
            // Masukkan proses yang sudah datang ke ready queue.
            // Gunakan "seen" agar tiap proses hanya dimasukkan sekali untuk momen AT awalnya.
            for(int i=0;i<n;i++){
                Proc p=procs.get(i);
                if(!seen[i] && p.at<=time){
                    rq.add(p);
                    seen[i]=true;
                }
            }

            // Jika queue kosong, loncat ke kedatangan berikutnya.
            // Ini berarti CPU idle di rentang [time..nextArrival). Dalam versi ini idle tidak dicatat sebagai segmen.
            if(rq.isEmpty()){
                OptionalInt next=nextArrivalAfter(procs,time);
                if(next.isPresent()){
                    time=next.getAsInt();
                    continue;
                } else {
                    break; // tidak ada proses tersisa
                }
            }

            // Ambil proses di depan queue
            Proc cur=rq.poll();
            if(cur.rt==0) continue; // berjaga jika ada yang sudah nol

            // Jalankan proses selama min(quantum, sisa)
            int run=Math.min(quantum,cur.rt);
            int start=time, end=time+run;

            // Update state proses
            cur.rt-=run;
            time=end;

            // Catat segmen eksekusi untuk Gantt
            segs.add(new Seg(cur.pid,start,end));

            // Masukkan proses yang mungkin baru datang selama eksekusi barusan
            for(int i=0;i<n;i++){
                Proc p=procs.get(i);
                if(!seen[i] && p.at<=time){
                    rq.add(p);
                    seen[i]=true;
                }
            }

            // Jika masih ada sisa maka proses kembali ke belakang queue
            if(cur.rt>0){
                rq.add(cur);
            } else {
                // Sudah selesai. Simpan completion time dan cek penghentian loop
                cur.ct=time;
                completed++;
                if(completed==n) break;
            }
        }
        return segs;
    }

    /** RR pengembangan.
     *  Dua ide utama.
     *  1. Setiap giliran memilih proses dengan sisa waktu paling kecil di antara yang siap.
     *     Ini membuat proses pendek cepat selesai walau tetap dibatasi oleh quantum aktif.
     *  2. Quantum digandakan setiap kali semua proses aktif pada periode itu
     *     sudah minimal mendapat jatah sekali. Variabel served mencatat siapa saja
     *     yang sudah kebagian dalam putaran saat ini.
     */
    private List<Seg> rrDeveloped(List<Proc> procs,int baseQ){
        List<Seg> segs=new ArrayList<>();
        int time=minArrival(procs); // waktu mulai simulasi
        int q=baseQ;                // quantum aktif, nanti bisa digandakan
        Set<String> served=new HashSet<>(); // proses yang sudah kebagian di putaran berjalan

        while(true){
            // Kumpulkan proses yang sudah siap di waktu "time"
            List<Proc> ready=new ArrayList<>();
            for(Proc p:procs) if(p.at<=time && p.rt>0) ready.add(p);

            // Jika belum ada yang siap, loncat ke kedatangan berikutnya
            if(ready.isEmpty()){
                OptionalInt next=nextArrivalAfter(procs,time);
                if(next.isPresent()){
                    time=next.getAsInt();
                    continue;
                } else {
                    break;
                }
            }

            // Pilih proses dengan sisa eksekusi paling kecil.
            // Jika sama. lihat AT lebih kecil dulu. lalu urut pid untuk stabilitas.
            ready.sort(Comparator.<Proc>comparingInt(p->p.rt)
                    .thenComparingInt(p->p.at)
                    .thenComparing(p->p.pid));
            Proc cur=ready.get(0);

            // Eksekusi dengan quantum aktif atau sisa yang lebih kecil
            int run=Math.min(q,cur.rt);
            int start=time, end=time+run;
            cur.rt-=run;
            time=end;

            segs.add(new Seg(cur.pid,start,end));
            served.add(cur.pid);
            if(cur.rt==0) cur.ct=time;

            // Cek apakah satu "putaran" selesai.
            // Putaran dianggap selesai jika semua proses yang masih aktif di waktu sekarang
            // sudah muncul di set "served". Ketika selesai, gandakan quantum.
            List<Proc> active=new ArrayList<>();
            for(Proc p:procs) if(p.rt>0 && p.at<=time) active.add(p);

            boolean roundDone=!active.isEmpty();
            if(roundDone){
                for(Proc p:active) if(!served.contains(p.pid)){ roundDone=false; break; }
            }
            if(roundDone){
                q*=2;          // quantum baru menjadi dua kali lipat
                served.clear();// mulai putaran baru
            }
        }
        return segs;
    }

    /**
     * Mengisi Completion Time (CT) tiap proses berdasarkan segmen eksekusi yang tercatat.
     * CT diambil dari waktu selesai terakhir proses yang muncul pada daftar segmen.
     * Jika CT suatu proses sudah terisi saat simulasi, nilai itu dipertahankan.
     */
    private void computeTimes(List<Proc> procs, List<Seg> segs) {
        // Peta dari pid ke waktu selesai terakhir yang terlihat di Gantt
        Map<String, Integer> lastEnd = new HashMap<>();
        for (Seg s : segs) {
            lastEnd.put(s.pid, s.end); // setiap kali pid muncul, end terbaru akan menimpa yang lama
        }

        // Isi CT hanya jika belum terisi. Ambil dari peta lastEnd
        for (Proc p : procs) {
            if (p.ct < 0 && lastEnd.containsKey(p.pid)) {
                p.ct = lastEnd.get(p.pid);
            }
        }
    }

    /**
     * Mengisi tabel hasil dengan WT, TAT, dan CT untuk setiap proses.
     * Rumus.
     *  TAT = CT − AT
     *  WT  = TAT − BT
     * Baris terakhir menampilkan rata-rata WT dan rata-rata TAT.
     */
    private void fillResultTable(List<Proc> procs){
        resultModel.setRowCount(0);

        double sumWT = 0;
        double sumTT = 0;

        for (Proc p : procs) {
            int finish = p.ct;           // CT = waktu selesai
            int tt = finish - p.at;      // TT = Turnaround Time
            int wt = tt - p.bt;          // WT = Waiting Time

            sumWT += wt;
            sumTT += tt;

            // urutan kolom: Proses | AT | BT | Finish | WT | TT
            resultModel.addRow(new Object[]{ p.pid, p.at, p.bt, finish, wt, tt });
        }

        int n = procs.size();
        String awt = n > 0 ? String.format(Locale.US, "%.2f", sumWT / n) : "-";
        String att = n > 0 ? String.format(Locale.US, "%.2f", sumTT / n) : "-";

        // baris ringkasan
        resultModel.addRow(new Object[]{ "Rata-rata", "", "", "", awt, att });
    }


    /**
     * Menampilkan data Gantt dalam bentuk teks sederhana.
     * Format setiap baris. [start..end] pid
     * Contoh. [4..8] p2
     */
    private void showGantt(List<Seg> segs) {
        StringBuilder sb = new StringBuilder();
        for (Seg s : segs) {
            sb.append(String.format("[%d..%d] %s%n", s.start, s.end, s.pid));
        }
        ganttText.setText(sb.toString());
    }

    // ===== Dataset contoh
    private void loadDataset1(){
        inputModel.setRowCount(0);
        for(int i=1;i<=5;i++) inputModel.addRow(new Object[]{"p"+i,0,6});
        quantumSpinner.setValue(4);
        algoCombo.setSelectedIndex(0);
    }
    private void loadDataset2(){
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"p1",0,2});
        inputModel.addRow(new Object[]{"p2",0,4});
        inputModel.addRow(new Object[]{"p3",0,6});
        inputModel.addRow(new Object[]{"p4",0,8});
        inputModel.addRow(new Object[]{"p5",0,20});
        quantumSpinner.setValue(4);
        algoCombo.setSelectedIndex(0);
    }
    private void loadDataset3(){
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"p1",0,7});
        inputModel.addRow(new Object[]{"p2",2,4});
        inputModel.addRow(new Object[]{"p3",4,9});
        inputModel.addRow(new Object[]{"p4",6,5});
        inputModel.addRow(new Object[]{"p5",8,3});
        quantumSpinner.setValue(3);
        algoCombo.setSelectedIndex(0);
    }
    private void loadDataset4(){
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"p1",0,15});
        inputModel.addRow(new Object[]{"p2",0,20});
        inputModel.addRow(new Object[]{"p3",5,2});
        inputModel.addRow(new Object[]{"p4",7,3});
        inputModel.addRow(new Object[]{"p5",4,7});
        quantumSpinner.setValue(4);
        algoCombo.setSelectedIndex(0);
    }

    private void loadDataset5(){
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"p1", 0, 25});
        inputModel.addRow(new Object[]{"p2", 3,  5});
        inputModel.addRow(new Object[]{"p3", 4,  7});
        inputModel.addRow(new Object[]{"p4", 10, 3});
        inputModel.addRow(new Object[]{"p5", 12, 18});
        quantumSpinner.setValue(5);
        algoCombo.setSelectedIndex(0);
    }


    // ===== JTable zebra sederhana
    static class StripedTable extends JTable {
        private final Color even = new Color(247, 250, 252);
        private final Color odd  = Color.WHITE;
        StripedTable(DefaultTableModel model){ super(model); }
        @Override
        public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            if (!isRowSelected(row)) c.setBackground((row % 2 == 0) ? even : odd);
            return c;
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new RoundRobinSchedulerApp().setVisible(true));
    }
}
