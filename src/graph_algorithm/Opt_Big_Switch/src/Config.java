/**
 * Author: Nanxi Kang (nkang@cs.princeton.edu) 
 * All rights reserved.
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;


public class Config {
	
	public static final String ALLOC_FILE_T = "%sAlloc%d.txt"; 
	public static final String ALLOC_FILE_P_T = "%sAlloc_p%d.txt";
	public static final String POLICY_NUM_FILE_T = "%sPolicyNum%d.txt";
	public static final String RATIO_FILE_T = "%sRatio%d.txt";
	public static final String RES_FILE_T = "%sRes%d.txt"; 
	public static final String RES_FILE_P_T = "%sRes%d_%d.txt";
	
	private static String PATH_POLICY_NUM_FILE_T = "%sPathNum.txt";

	private String dir;
	public String pathPolicyFile;
	public String pathPolicyNumFile;
	public String topoFile;
	
	public double[][] eta = {{1.0, 1.4, 1.7, 2.0, 2.5, 3.0, 3.5, 4.0},
	/*	
		{1.0, 1.15, 1.3, 1.5, .17, 2., 3.0, 3.5, 4.0, 4.5, 5.0}, //20k 3
		{1.0, 1.4, 1.7, 2.2, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0}, //25k 4
		{1.0, 1.4, 1.7, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0}, //30k 5*/
	};
	public int start_itr = 0;
	public int end_itr = 100;
	public int processNum = 1;
	
		
	public void load(String[] args) throws Exception {
	   dir = args[0];
	   pathPolicyFile = args[1];
	   topoFile = args[2];
	   pathPolicyNumFile = String.format(PATH_POLICY_NUM_FILE_T, dir);
	   createPathPolicyNumFile(pathPolicyFile, pathPolicyNumFile);
	   
	   int idx;
	   idx = Utils.locate(args, "-p");
	   if (idx >= 0 && idx + 1 < args.length) {
		   processNum = Integer.parseInt(args[idx + 1]);
	   }
	   
	   idx = Utils.locate(args, "-i");
	   if (idx >= 0 && idx + 2 < args.length) {
		   start_itr = Integer.parseInt(args[idx + 1]);
		   end_itr = Integer.parseInt(args[idx + 2]);
	   }
	   
	   idx = Utils.locate(args, "-e");
	   if (idx >= 0 && idx + 1 < args.length) {
		   BufferedReader f_eta = new BufferedReader(new FileReader(args[idx + 1]));
		   f_eta.readLine();
		   f_eta.close();
	   }
	   
	   
	}
	
	
	private void createPathPolicyNumFile(String policyFile, String policyNumFile) throws Exception{
		BufferedReader fin = new BufferedReader(new FileReader(policyFile));
		PrintWriter fout = new PrintWriter(new FileWriter(policyNumFile));
		
		int n_policy = Integer.parseInt(fin.readLine());
		fout.println(n_policy);
		for (int i = 0; i < n_policy; ++i) {
			fin.readLine();
			int n_rule = Integer.parseInt(fin.readLine());
			fout.println(n_rule);
			for (int j = 0; j < n_rule; ++j)
				fin.readLine();
		}
		fin.close();
		fout.close();
	}
	
	
	public String ratioFile(int k) {
		return String.format(dir, RATIO_FILE_T, k);
	}
	
	public String policyNumFile(int k) {
		return String.format(dir, POLICY_NUM_FILE_T, k);
	}
	
	public String allocFile(int k) {
		return String.format(dir, ALLOC_FILE_T, k);
	}
	
	public String allocFile(int k, int p) {
		return String.format(dir, ALLOC_FILE_P_T, p);
	}
	
	public String resFile(int k) {
		return String.format(dir, RES_FILE_T, k);
	}
	
	public String resFile(int k, int p) {
		return String.format(dir, RES_FILE_P_T, k, p);
	}
	
	
}
