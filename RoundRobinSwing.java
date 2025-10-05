import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class RoundRobinSwing extends JFrame {

    // ... (Kelas Process, GanttBlock, SimulationResult tetap sama, tidak perlu diubah) ...
    static class Process {
        String name;
        int arrivalTime;
        int burstTime;
        int remainingBurstTime;
        int completionTime;
        int turnaroundTime;
        int waitingTime;

        public Process(String name, int arrivalTime, int burstTime) {
            this.name = name;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.remainingBurstTime = burstTime;
        }

        void reset() {
            this.remainingBurstTime = burstTime;
            this.completionTime = 0;
            this.turnaroundTime = 0;
            this.waitingTime = 0;
        }
    }

    private record GanttBlock(String processName, int start, int end) {}

    private record SimulationResult(List<Process> finalProcesses, List<GanttBlock> ganttChart, double awt, double atat) {}

    // Record untuk menyimpan detail dataset termasuk default quantum
    private record Dataset(List<Object[]> processes, int defaultQuantum) {}

    private JTable inputTable, outputTable;
    private JTextField quantumField;
    private JComboBox<String> methodSelector;
    private JTextArea ganttArea;
    private JLabel statsLabel;

    private Map<String, Dataset> predefinedDatasets;
    private JComboBox<String> datasetSelector;

    public RoundRobinSwing() {
        setTitle("Simulasi Penjadwalan Round Robin (Swing)");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ================== DEFINISI FONT DAN STYLE ==================
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 16);
        Font boldFont = new Font("Segoe UI", Font.BOLD, 16);
        Font titleFont = new Font("Segoe UI", Font.BOLD, 18);
        Font monoFont = new Font("Consolas", Font.PLAIN, 15);
        int tableRowHeight = 25;

        createPredefinedDatasets();

        // ================== INPUT PANEL ==================
        JPanel inputPanel = new JPanel(new BorderLayout(0, 10));
        TitledBorder inputBorder = new TitledBorder("1. Input Proses");
        inputBorder.setTitleFont(titleFont);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 5), inputBorder));

        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel datasetLabel = new JLabel("Pilih Dataset:");
        datasetLabel.setFont(mainFont);
        selectorPanel.add(datasetLabel);
        datasetSelector = new JComboBox<>(predefinedDatasets.keySet().toArray(new String[0]));
        datasetSelector.setFont(mainFont);
        selectorPanel.add(datasetSelector);
        inputPanel.add(selectorPanel, BorderLayout.NORTH);

        String[] inputCols = {"Proses", "Arrival Time", "Burst Time"};
        DefaultTableModel inputModel = new DefaultTableModel(inputCols, 0);
        inputTable = new JTable(inputModel);

        // MODIFIKASI: Mengatur font dan tinggi baris tabel input
        inputTable.setFont(mainFont);
        inputTable.getTableHeader().setFont(boldFont);
        inputTable.setRowHeight(tableRowHeight);
        // PERBAIKAN: Membuat garis grid menjadi tipis dan seragam
        inputTable.setGridColor(Color.LIGHT_GRAY);
        inputTable.setIntercellSpacing(new Dimension(1, 1));


        JScrollPane inputScroll = new JScrollPane(inputTable);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Tambah Proses");
        JButton removeBtn = new JButton("Hapus Proses");
        addBtn.setFont(mainFont);
        removeBtn.setFont(mainFont);
        btnPanel.add(addBtn);
        btnPanel.add(removeBtn);
        inputPanel.add(btnPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> {
            int index = inputModel.getRowCount() + 1;
            inputModel.addRow(new Object[]{"P" + index, 0, 0});
        });
        removeBtn.addActionListener(e -> {
            int row = inputTable.getSelectedRow();
            if (row >= 0) inputModel.removeRow(row);
        });

        datasetSelector.addActionListener(e -> {
            String selectedDataset = (String) datasetSelector.getSelectedItem();
            loadDataset(selectedDataset);
        });

        // ================== CONFIG PANEL ==================
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        TitledBorder configBorder = new TitledBorder("2. Konfigurasi Simulasi");
        configBorder.setTitleFont(titleFont);
        configPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(5, 10, 10, 10), configBorder));

        methodSelector = new JComboBox<>(new String[]{
                "Round Robin Umum (Standar)",
                "Pengembangan Round Robin (Modifikasi)"
        });
        methodSelector.setFont(mainFont);

        quantumField = new JTextField("3", 5);
        quantumField.setFont(mainFont);
        JButton simulateBtn = new JButton("JALANKAN SIMULASI");
        simulateBtn.setFont(boldFont);

        JLabel methodLabel = new JLabel("Metode:");
        methodLabel.setFont(mainFont);
        JLabel quantumLabel = new JLabel("Time Quantum:");
        quantumLabel.setFont(mainFont);

        configPanel.add(methodLabel);
        configPanel.add(methodSelector);
        configPanel.add(quantumLabel);
        configPanel.add(quantumField);
        configPanel.add(simulateBtn);

        loadDataset((String) datasetSelector.getSelectedItem());

        // ================== OUTPUT PANEL ==================
        JPanel outputPanel = new JPanel(new BorderLayout(10, 10));
        TitledBorder outputBorder = new TitledBorder("3. Hasil Simulasi");
        outputBorder.setTitleFont(titleFont);
        outputPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 5, 10, 10), outputBorder));

        String[] outputCols = {"Proses", "Completion Time", "Turnaround Time", "Waiting Time"};
        DefaultTableModel outputModel = new DefaultTableModel(outputCols, 0);
        outputTable = new JTable(outputModel);

        // MODIFIKASI: Mengatur font dan tinggi baris tabel output
        outputTable.setFont(mainFont);
        outputTable.getTableHeader().setFont(boldFont);
        outputTable.setRowHeight(tableRowHeight);
        // PERBAIKAN: Membuat garis grid menjadi tipis dan seragam
        outputTable.setGridColor(Color.LIGHT_GRAY);
        outputTable.setIntercellSpacing(new Dimension(1, 1));

        statsLabel = new JLabel("AWT: - | ATAT: -", SwingConstants.CENTER);
        statsLabel.setFont(boldFont);
        statsLabel.setBorder(new EmptyBorder(5,0,5,0));

        ganttArea = new JTextArea(10, 1);
        ganttArea.setEditable(false);
        ganttArea.setFont(monoFont);

        outputPanel.add(new JScrollPane(outputTable), BorderLayout.CENTER);
        outputPanel.add(statsLabel, BorderLayout.NORTH);
        outputPanel.add(new JScrollPane(ganttArea), BorderLayout.SOUTH);

        // ================== MAIN LAYOUT ==================
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputPanel);
        splitPane.setDividerLocation(550);

        add(splitPane, BorderLayout.CENTER);
        add(configPanel, BorderLayout.SOUTH);

        // ================== ACTION SIMULASI ==================
        simulateBtn.addActionListener(e -> {
            try {
                int quantum = Integer.parseInt(quantumField.getText());
                if (quantum <= 0) throw new NumberFormatException();

                List<Process> processes = new ArrayList<>();
                for (int i = 0; i < inputModel.getRowCount(); i++) {
                    String name = inputModel.getValueAt(i, 0).toString();
                    int at = Integer.parseInt(inputModel.getValueAt(i, 1).toString());
                    int bt = Integer.parseInt(inputModel.getValueAt(i, 2).toString());
                    processes.add(new Process(name, at, bt));
                }

                processes.forEach(Process::reset);
                SimulationResult result = methodSelector.getSelectedIndex() == 0
                        ? runStandardRoundRobin(processes, quantum)
                        : runEnhancedRoundRobin(processes, quantum);

                outputModel.setRowCount(0);
                for (Process p : result.finalProcesses()) {
                    outputModel.addRow(new Object[]{
                            p.name, p.completionTime, p.turnaroundTime, p.waitingTime
                    });
                }

                statsLabel.setText(String.format("Average Waiting Time (AWT): %.2f | Average Turnaround Time (ATAT): %.2f",
                        result.awt(), result.atat()));

                StringBuilder ganttText = new StringBuilder();
                ganttText.append("Waktu Mulai -> Selesai :: Proses\n");
                ganttText.append("---------------------------------\n");
                for (GanttBlock b : result.ganttChart()) {
                    ganttText.append(String.format("   %3d -> %-5d :: %s\n", b.start(), b.end(), b.processName()));
                }
                ganttArea.setText(ganttText.toString());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Input tidak valid! Pastikan semua nilai adalah angka yang benar.", "Error Input", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void createPredefinedDatasets() {
        predefinedDatasets = new LinkedHashMap<>();

        // Dataset 1
        List<Object[]> ds1 = new ArrayList<>();
        for (int i = 1; i <= 5; i++) ds1.add(new Object[]{"p" + i, 0, 6});
        predefinedDatasets.put("Dataset 1", new Dataset(ds1, 4));

        // Dataset 2
        List<Object[]> ds2 = new ArrayList<>();
        ds2.add(new Object[]{"p1", 0, 2});
        ds2.add(new Object[]{"p2", 0, 4});
        ds2.add(new Object[]{"p3", 0, 6});
        ds2.add(new Object[]{"p4", 0, 8});
        ds2.add(new Object[]{"p5", 0, 20});
        predefinedDatasets.put("Dataset 2", new Dataset(ds2, 4));

        // Dataset 3 (Data Asli dari soal Anda sebelumnya)
        List<Object[]> ds3 = new ArrayList<>();
        ds3.add(new Object[]{"p1", 0, 7});
        ds3.add(new Object[]{"p2", 2, 4});
        ds3.add(new Object[]{"p3", 4, 9});
        ds3.add(new Object[]{"p4", 6, 5});
        ds3.add(new Object[]{"p5", 8, 3});
        predefinedDatasets.put("Dataset 3", new Dataset(ds3, 3));

        // Dataset 4
        List<Object[]> ds4 = new ArrayList<>();
        ds4.add(new Object[]{"p1", 0, 15});
        ds4.add(new Object[]{"p2", 0, 20});
        ds4.add(new Object[]{"p3", 5, 2});
        ds4.add(new Object[]{"p4", 7, 3});
        ds4.add(new Object[]{"p5", 4, 7});
        predefinedDatasets.put("Dataset 4", new Dataset(ds4, 4));

        // Dataset 5
        List<Object[]> ds5 = new ArrayList<>();
        ds5.add(new Object[]{"p1", 0, 25});
        ds5.add(new Object[]{"p2", 3, 5});
        ds5.add(new Object[]{"p3", 4, 7});
        ds5.add(new Object[]{"p4", 10, 3});
        ds5.add(new Object[]{"p5", 12, 18});
        predefinedDatasets.put("Dataset 5", new Dataset(ds5, 5));

        // Dataset 6 (Data seimbang tanpa Arrival Time)
        List<Object[]> ds6 = new ArrayList<>();
        ds6.add(new Object[]{"p1", 0, 12});
        ds6.add(new Object[]{"p2", 0, 8});
        ds6.add(new Object[]{"p3", 0, 5});
        ds6.add(new Object[]{"p4", 0, 2});
        ds6.add(new Object[]{"p5", 0, 1});
        predefinedDatasets.put("Data Seimbang (tanpa AT)", new Dataset(ds6, 3));

// Dataset 7 (Data jurnal perbandingan)
        List<Object[]> ds7 = new ArrayList<>();
        ds7.add(new Object[]{"p1", 0, 12});
        ds7.add(new Object[]{"p2", 2, 8});
        ds7.add(new Object[]{"p3", 3, 5});
        ds7.add(new Object[]{"p4", 5, 2});
        ds7.add(new Object[]{"p5", 9, 1});
        predefinedDatasets.put("Data Jurnal (AT dan BT berbeda)", new Dataset(ds7, 3));

// Dataset 8 (Data relevan preemption)
        List<Object[]> ds8 = new ArrayList<>();
        ds8.add(new Object[]{"p1", 0, 10});
        ds8.add(new Object[]{"p2", 2, 4});
        ds8.add(new Object[]{"p3", 4, 6});
        ds8.add(new Object[]{"p4", 6, 3});
        ds8.add(new Object[]{"p5", 8, 2});
        predefinedDatasets.put("Data Relevan (Preemption AT)", new Dataset(ds8, 3));

    }

    private void loadDataset(String datasetName) {
        DefaultTableModel model = (DefaultTableModel) inputTable.getModel();
        model.setRowCount(0);

        Dataset selectedData = predefinedDatasets.get(datasetName);
        if (selectedData != null) {
            for (Object[] rowData : selectedData.processes()) {
                model.addRow(rowData);
            }
            quantumField.setText(String.valueOf(selectedData.defaultQuantum()));
            methodSelector.setSelectedIndex(0);
        }
    }

    // ... (Semua method algoritma di bawah ini TETAP SAMA) ...
    private SimulationResult runStandardRoundRobin(List<Process> processes, int quantum) {
        List<Process> processList = processes.stream()
                .map(p -> new Process(p.name, p.arrivalTime, p.burstTime))
                .sorted(Comparator.comparingInt(p -> p.arrivalTime))
                .collect(Collectors.toList());

        Queue<Process> readyQueue = new LinkedList<>();
        List<GanttBlock> ganttChart = new ArrayList<>();
        int currentTime = 0, completed = 0;
        List<Process> jobQueue = new ArrayList<>(processList);

        while (completed < processList.size()) {
            Iterator<Process> it = jobQueue.iterator();
            while (it.hasNext()) {
                Process p = it.next();
                if (p.arrivalTime <= currentTime) {
                    readyQueue.add(p);
                    it.remove();
                }
            }
            if (readyQueue.isEmpty()) {
                if (jobQueue.isEmpty()) break;
                currentTime++;
                continue;
            }
            Process cur = readyQueue.poll();
            int start = currentTime;
            int exec = Math.min(quantum, cur.remainingBurstTime);
            currentTime += exec;
            cur.remainingBurstTime -= exec;
            ganttChart.add(new GanttBlock(cur.name, start, currentTime));

            it = jobQueue.iterator();
            while (it.hasNext()) {
                Process p = it.next();
                if (p.arrivalTime <= currentTime) {
                    readyQueue.add(p);
                    it.remove();
                }
            }
            if (cur.remainingBurstTime > 0) {
                readyQueue.add(cur);
            } else {
                completed++;
                cur.completionTime = currentTime;
                cur.turnaroundTime = cur.completionTime - cur.arrivalTime;
                cur.waitingTime = cur.turnaroundTime - cur.burstTime;
            }
        }
        double awt = processList.stream().mapToInt(p -> p.waitingTime).average().orElse(0);
        double atat = processList.stream().mapToInt(p -> p.turnaroundTime).average().orElse(0);
        return new SimulationResult(processList, ganttChart, awt, atat);
    }

    private void admitArrivals(List<Process> job, ArrayDeque<Process> rq, int time) {
        Iterator<Process> it = job.iterator();
        while (it.hasNext()) {
            Process p = it.next();
            if (p.arrivalTime <= time) {
                rq.add(p);
                it.remove();
            }
        }
    }

    // ==========================================================
// Versi runEnhancedRoundRobin lengkap yang pakai fungsi di atas
// ==========================================================
    private SimulationResult runEnhancedRoundRobin(List<Process> processes, int baseQ) {
        // clone input dan urutkan AT
        List<Process> ps = processes.stream()
                .map(p -> new Process(p.name, p.arrivalTime, p.burstTime))
                .sorted(Comparator.comparingInt(p -> p.arrivalTime))
                .collect(Collectors.toList());

        List<GanttBlock> gantt = new ArrayList<>();
        int time = ps.stream().mapToInt(p -> p.arrivalTime).min().orElse(0);

        // ========= SIKLUS 1. RR berbasis AT. boleh preempt saat ada arrival baru =========
        Set<String> servedFirst = new HashSet<>();
        ArrayDeque<Process> rq = new ArrayDeque<>();
        List<Process> job = new ArrayList<>(ps); // yang belum masuk ready

        // panggil helper untuk menambahkan proses yang sudah datang
        admitArrivals(job, rq, time);

        while (servedFirst.size() < ps.size()) {
            if (rq.isEmpty()) {
                int nextAT = job.stream().mapToInt(p -> p.arrivalTime).min().orElse(Integer.MAX_VALUE);
                if (nextAT == Integer.MAX_VALUE) break;
                time = nextAT;
                admitArrivals(job, rq, time);
                continue;
            }

            Process cur = rq.poll();
            if (cur.remainingBurstTime <= 0) continue;

            int q = baseQ;
            int sliceLeft = q;

            while (sliceLeft > 0 && cur.remainingBurstTime > 0) {
                OptionalInt nextAT = job.stream().mapToInt(p -> p.arrivalTime).min();
                int run;
                if (nextAT.isPresent() && nextAT.getAsInt() > time && nextAT.getAsInt() - time < sliceLeft) {
                    run = Math.min(cur.remainingBurstTime, nextAT.getAsInt() - time);
                } else {
                    run = Math.min(cur.remainingBurstTime, sliceLeft);
                }

                if (run == 0) {
                    time = nextAT.getAsInt();
                    admitArrivals(job, rq, time);
                    break;
                }

                int start = time, end = time + run;
                cur.remainingBurstTime -= run;
                time = end;
                gantt.add(new GanttBlock(cur.name, start, end));
                sliceLeft -= run;

                admitArrivals(job, rq, time);
                if (!job.isEmpty()) {
                    int nearest = job.stream().mapToInt(p -> p.arrivalTime).min().orElse(Integer.MAX_VALUE);
                    if (nearest == time) {
                        admitArrivals(job, rq, time);
                        break;
                    }
                }
            }

            servedFirst.add(cur.name);
            if (cur.remainingBurstTime > 0) rq.add(cur);
            else cur.completionTime = time;

            admitArrivals(job, rq, time);
        }

// ========= SIKLUS 2+. quantum digandakan. pilih sisa terpendek =========
        int q = baseQ * 2;

        while (true) {
            // snapshot time agar final untuk lambda
            final int tNow = time;

            List<Process> active = ps.stream()
                    .filter(p -> p.remainingBurstTime > 0 && p.arrivalTime <= tNow)
                    .collect(Collectors.toList());

            // jika tidak ada yang aktif, loncat ke arrival berikutnya atau selesai
            if (active.isEmpty()) {
                final int tN = time; // snapshot lagi untuk lambda di bawah
                OptionalInt nextAT = ps.stream()
                        .filter(p -> p.remainingBurstTime > 0 && p.arrivalTime > tN)
                        .mapToInt(p -> p.arrivalTime)
                        .min();

                if (nextAT.isPresent()) {
                    time = nextAT.getAsInt();
                    continue;
                }
                break; // semua selesai
            }

            // jalankan satu putaran. semua aktif minimal 1 kali
            Set<String> served = new HashSet<>();
            while (served.size() < active.size()) {
                Process cur = active.stream()
                        .filter(p -> p.remainingBurstTime > 0)
                        .min(Comparator.comparingInt((Process p) -> p.remainingBurstTime)
                                .thenComparingInt(p -> p.arrivalTime)
                                .thenComparing(p -> p.name))
                        .orElse(null);
                if (cur == null) break;

                int run = Math.min(q, cur.remainingBurstTime);
                int start = time, end = time + run;
                cur.remainingBurstTime -= run;
                time = end;
                gantt.add(new GanttBlock(cur.name, start, end));
                served.add(cur.name);
                if (cur.remainingBurstTime == 0) cur.completionTime = time;
            }

            // akhir siklus. gandakan quantum
            q *= 2;
        }


        // hitung TAT dan WT
        for (Process p : ps) {
            p.turnaroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;
        }

        double awt = ps.stream().mapToInt(p -> p.waitingTime).average().orElse(0);
        double atat = ps.stream().mapToInt(p -> p.turnaroundTime).average().orElse(0);
        return new SimulationResult(ps, gantt, awt, atat);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new RoundRobinSwing().setVisible(true));
    }
}
