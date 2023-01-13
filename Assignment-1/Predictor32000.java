
public class Predictor32000 extends Predictor {
	private Table pap_table;
	private Table gshare_table;
	private Table tournament_table;
	private Table pap_ghr;
	private Table gshare_ghr;
	int pap_n = 11; // 9 for GShare, 8 for Pap, 11
	int gshare_n = 13;
	int pap_k = 3;
	int gshare_k = 13;
	int n1 = 8; // 7 for Pap, 9 for GShare
	int shift = 0;
	int tournament_n = 12;
	public Predictor32000() {
		gshare_table = new Table(1 << Math.max(gshare_n, gshare_k), 2);
		// pap_table = new Table(1 << pap_n + pap_k, 2);
		pap_table = new Table(1 << Math.max(pap_n, pap_k), 2);
		tournament_table = new Table(1 << tournament_n, 2);
		gshare_ghr = new Table(1, gshare_k);
		pap_ghr = new Table(1 << n1, pap_k);
	}

	// public void train_saturating_counter(long address, boolean outcome, boolean predict){
	// 	long index = address & ((1 << sc_n) - 1);
	// 	int val = sc_table.getInteger((int)index, 0, 1);
	// 	if(outcome){
	// 		sc_table.setInteger((int)index, 0, 1, Math.min(3, val + 1));
	// 	}
	// 	else{
	// 		sc_table.setInteger((int)index, 0, 1, Math.max(0, val - 1));
	// 	}
	// }

	public void train_pap(long address, boolean outcome, boolean predict){
		int temp = 0;
		if(outcome){
			temp = 1;
		}
		long lsb = address & ((1 << pap_n) - 1);
		address = address << shift;
		long ghr_index = address & ((1 << n1) - 1);
		int ghr_output = pap_ghr.getInteger((int)ghr_index, 0, pap_k - 1);
		pap_ghr.setInteger((int)ghr_index, 0, pap_k - 1, (ghr_output << 1) + temp);
		long index = lsb^ghr_output;
		// long index = (lsb << pap_k) + ghr_output;
		int val = pap_table.getInteger((int)index, 0, 1);
		if(outcome){
			pap_table.setInteger((int)index, 0, 1, Math.min(3, val + 1));
		}
		else{
			pap_table.setInteger((int)index, 0, 1, Math.max(0, val - 1));
		}
	}

	public void train_gshare(long address, boolean outcome, boolean predict){
		int temp = 0;
		if(outcome){
			temp = 1;
		}
		long lsb = address & ((1 << gshare_n) - 1);
		address = address << shift;
		int ghr_output = gshare_ghr.getInteger(0, 0, gshare_k - 1);
		gshare_ghr.setInteger(0, 0, gshare_k - 1, (ghr_output << 1) + temp);
		long index = lsb^ghr_output;
		// long index = (lsb << k) + ghr_output;
		int val = gshare_table.getInteger((int)index, 0, 1);
		if(outcome){
			gshare_table.setInteger((int)index, 0, 1, Math.min(3, val + 1));
		}
		else{
			gshare_table.setInteger((int)index, 0, 1, Math.max(0, val - 1));
		}
	}

	public void train_tournament(long address, boolean outcome, boolean predict){
		boolean predict1 = predict_gshare(address);
		boolean predict2 = predict_pap(address);
		if(predict1 != predict2){
			long index = address & ((1 << tournament_n) - 1);
			int val = tournament_table.getInteger((int)index, 0, 1);
			if(predict1 == outcome){
				// System.out.print(predict2);
				// System.out.print(" - ");
				// System.out.println(outcome);
				tournament_table.setInteger((int)index, 0, 1, Math.max(0, val - 1));
			}
			else{
				// System.out.print(predict1);
				// System.out.print(" - ");
				// System.out.println(outcome);
				tournament_table.setInteger((int)index, 0, 1, Math.min(3, val + 1));
			}
		}
		train_gshare(address, outcome, predict);
		train_pap(address, outcome, predict);
	}

	public void Train(long address, boolean outcome, boolean predict) {
		train_tournament(address, outcome, predict);
	}

	// public boolean predict_saturating_counter(long address){
	// 	long index = address & ((1 << sc_n) - 1);
	// 	int val = sc_table.getInteger((int)index, 0, 1);
	// 	if(val == 0 || val == 1){
	// 		return false;
	// 	}
	// 	else{
	// 		return true;
	// 	}
	// }

	public boolean predict_pap(long address){
		long lsb = address & ((1 << pap_n) - 1);
		address = address << shift;
		long ghr_index = address & ((1 << n1) - 1);
		int ghr_output = pap_ghr.getInteger((int)ghr_index, 0, pap_k - 1);
		long index = lsb^ghr_output;
		// long index = (lsb << pap_k) + ghr_output;
		int val = pap_table.getInteger((int)index, 0, 1);
		if(val == 0 || val == 1){
			return false;
		}
		else{
			return true;
		}
	}

	public boolean predict_gshare(long address){
		long lsb = address & ((1 << gshare_n) - 1);
		int ghr_output = gshare_ghr.getInteger(0, 0, gshare_k - 1);
		long index = lsb^ghr_output;
		// long index = (lsb << k) + ghr_output;
		int val = gshare_table.getInteger((int)index, 0, 1);
		if(val == 0 || val == 1){
			return false;
		}
		else{
			return true;
		}
	}

	public boolean predict_tournament(long address){
		long index = address & ((1 << tournament_n) - 1);
		int val = tournament_table.getInteger((int)index, 0, 1);
		if(val == 2 || val == 3){
			return predict_pap(address);
		}
		else{
			return predict_gshare(address);
		}
	}

	public boolean predict(long address){
		return predict_tournament(address);
	}

}