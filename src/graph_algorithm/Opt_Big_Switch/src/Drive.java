import java.io.File;

public class Drive {
	
	public static String INPUT = "";
	
	//public static int TS_MIN = 0;
	//public static int TS_MAX = 9;

	public static boolean SAVE_ALLOC = true;
	
	public static double RATIO[][] ={
		{1.0, 1.4, 1.7, 2.0, 2.5, 3.0, 3.5, 4.0},
	/*	
		{1.0, 1.15, 1.3, 1.5, .17, 2., 3.0, 3.5, 4.0, 4.5, 5.0}, //20k 3
		{1.0, 1.4, 1.7, 2.2, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0}, //25k 4
		{1.0, 1.4, 1.7, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0}, //30k 5*/
	};
		
	public static String EXEDIR = "/Users/nanxikang/Documents/Research/github/policy_trans/src/exe/";
	
	/*** Iteration ***/
	public static int START = 0;
	public static int BOUNDNESS = START + 80;//30;
	public static double EXPAND_RATIO = 1.04;//1.0001;
	

	/***Multi-Dim****/
	public static String FOLDER = "/Users/nanxikang/Documents/Research/github/policy_trans/graph/path_input/";//graph_input/";
	public static String DIR = "/Users/nanxikang/Documents/Research/github/policy_trans/src/scripts/Graph1/";
	
	public static String PATH_POLICY_FILE_T = FOLDER + "Path_%s_%d.txt";
	public static String PATH_POLICY_NUM_FILE_T = FOLDER + "PathNum_%s_%d.txt";
	public static String TOPOLOGY_FILE_T = FOLDER + "Topology_%s_%d.txt";
	

  /**** Intermediate result *****/	
	
	public static String PATH_POLICY_FILE;// = DIR + "Path.txt";
	public static String PATH_POLICY_NUM_FILE;// = DIR + "PathNum.txt";
	public static String TOPOLOGY_FILE;// = DIR + "Topology.txt";
	
	
	
	/***Command Line***/
	public static String PROVISION_COMM = "java -jar " + EXEDIR + "provision_Edge.jar %s %d %s %s";
	public static String DFS_COMM = EXEDIR + "batch_DFS_Set %s %s %s";
	public static String DFS_COMM_P = EXEDIR + "batch_DFS_MP %s %s %s %d %d";
	
	public static void main(String args[]) {
		try {
			Config config = new Config();
			config.load(args);			
			
			Procedure.RATIO = config.eta[0];

			PATH_POLICY_FILE = config.pathPolicyFile;
			PATH_POLICY_NUM_FILE = config.pathPolicyNumFile;
			TOPOLOGY_FILE = config.topoFile;
			ResultRecord r = work(config);
			System.out.println("It takes " + r.Iterations + " iterations to complete.");
			System.out.println("In one iteration, LP takes " + r.timeLP + " seconds, and path algorithm takes " + r.timeSolver + " seconds");
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	//Input: Global Policy, TOPOLOGY
	//Output: xxxxx
	public static ResultRecord work(Config config) {
		ResultRecord ret = new ResultRecord();
		ret.Iterations = -1;

		try {
			int start = config.start_itr;
			int end = config.end_itr;
			if (start == 0)
				initRatio(config);
			
			int procNum = config.processNum;
			
			// Declare output files
			String now_policyNum, now_alloc, now_res, now_ratio, next_ratio;
			String[] resFiles = new String[procNum];
			String[] allocFiles = new String[procNum];
						
			// Decalre command line & process			
			String prCommandLine;
			Process provision;
			
			String[] batchCommLines = new String[procNum];
			Process[] batches = new Process[procNum];
			
			int round, exitVal;
			int status;
			
			for (round = start; round < end; ++round) {
				now_policyNum = config.policyNumFile(round);
				now_alloc = config.allocFile(round);
				now_res = config.resFile(round);
				now_ratio = config.ratioFile(round);
				next_ratio = config.ratioFile(round + 1); 
				for (int i = 0; i < procNum; ++i) {
					resFiles[i] = config.resFile(round, i);
					allocFiles[i] =config.resFile(round, i); 
				}	
						
				//Generate estimated policy_num
				//PATH_POLICY + RATIO -> POLICY_NUM
				System.out.println("Generate Policy Num...");
				Procedure.Control(PATH_POLICY_NUM_FILE, now_ratio, now_policyNum);
				
				//Call LP, provision switch capacities for paths
				//TOPOLOGY + POLICY_NUM_FILE -> ALLOC
				prCommandLine = String.format(PROVISION_COMM, TOPOLOGY_FILE, now_policyNum, now_alloc);
				System.out.println("Run Provision..");
				System.out.println(prCommandLine);		
				long startProvision = System.currentTimeMillis();
				provision = Runtime.getRuntime().exec(prCommandLine);
				exitVal = provision.waitFor();
				long endProvision = System.currentTimeMillis();
				ret.timeLP = (endProvision - startProvision) * 1.0 / 1000;
				
				if (exitVal == 0) {
					//works out whether capacities can satisfy paths
					//ALLOC +  PATH_POLICY_FILE -> RES
					long startSolver = 0, endSolver = 0;
						
					// Copy Input for multi-process
					for (int i = 0; i < procNum; ++i) {
						batchCommLines[i] = String.format(DFS_COMM_P, allocFiles[i], PATH_POLICY_FILE, resFiles[i], procNum, i);
							
						File src = new File(now_alloc);
						File dst = new File(allocFiles[i]);
						Utils.copyFile(src, dst);
					}
					System.out.println("BATCH...");
					for (int i = 0; i < procNum; ++i) 
						System.out.println(batchCommLines[i]);
	
					startSolver = System.currentTimeMillis();
						
					/*for (int i = 0; i < procNum; ++i) {
						batches[i] = Runtime.getRuntime().exec(batchCommLines[i]);
					}
						
					for (int i = 0; i < procNum; ++i) {
						exitVal = batches[i].waitFor();
					}*/
					
					endSolver = System.currentTimeMillis();
										 
					ret.timeSolver = (endSolver - startSolver) * 1.0 / 1000;
					
					System.out.println("Run...DONE");
					
					//check feasibility
					//RES + RATIO -> RATIO'
					System.out.println("Check and Adjust");
					status = Procedure.CheckAdjustP(resFiles, now_res, now_ratio, next_ratio);
				}
				else status = -1;
				
				if (status == 0) {
					System.out.println("Round " + round + " : Try Again");
					++round;
					now_ratio = next_ratio;
				}				
				else {
					if (status > 0) {
						ret.Iterations = round;
						System.out.println("Round " + round + " : Success :)");
					}
					else 
						System.out.println("Round " + round + " : Failed...");
					break;
				}
			}
			System.out.println("Done!");
			
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
	}
		
	//PATH_POLICY -> RATIO
	private static void initRatio(Config config) throws Exception {
		String now_ratio = config.ratioFile(0); 
		System.out.println("Init Ratio...");
		Procedure.InitRatio(PATH_POLICY_NUM_FILE, TOPOLOGY_FILE, now_ratio);
	}
}
