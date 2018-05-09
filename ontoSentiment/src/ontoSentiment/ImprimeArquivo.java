package ontoSentiment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class ImprimeArquivo extends Thread{

	private String nome;
	ArrayList<TweetTraduzido> dados;
	
	public ImprimeArquivo(String nome, ArrayList<TweetTraduzido> dados){
		this.nome = nome;
		this.dados = dados;
	}
	public void run() {
		try {
			FileWriter arquivo = new FileWriter(new File("C:/Users/Raimundo/Desktop/"+nome+".csv"));
			//BufferedWriter arquivo = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("C:/Users/Raimundo/Desktop/"+nome+".txt"),"ISO-8859-1"));
			String print = "";
			for(TweetTraduzido d : dados){
				print = d.getPt()+";";
				
				if(d.getEnGoogle() !=null && !d.getEnGoogle().equals(""))
					print += d.getEnGoogle()+";";
				else
					print += "<none>;";
				
				if(d.getEnYandex() !=null && !d.getEnYandex().equals(""))
					print += d.getEnYandex()+";";
				else
					print += "<none>;";
				
				print +=d.getClassifiedGoogle()+";";
				print +=d.getClassifiedYandex();				
				arquivo.write(print+";\n");
				
			}
			arquivo.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
