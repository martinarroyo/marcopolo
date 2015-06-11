package marcobinding;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

public class Main {

	public static void main(String[] args) {
		Marco m = new Marco();
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		m.request_for(nodes, "marcobootstrap2", 0, null, null, 0, 0);
		nodes.clear();
		
		if(-1 == m.marco(nodes, 0, null, null, 0, 0)){
			System.out.println("Error");
			return;
		}
		
		for(Node n : nodes){
			System.out.println(n.getAddress());
			Iterator<Entry<String, Parameter>> it = n.getParams().entrySet().iterator();
			while(it.hasNext()){
				Entry<String, Parameter> pair = it.next();
				
				if(((Parameter)pair.getValue()).type == Marco.TYPE_STRING){
					System.out.println(pair.getKey() + ":"+((Parameter)pair.getValue()).value);
				}
			}
		}

	}

}