import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class testClass {
	public static void main(String __[]){
		JFrame f = new JFrame("");
		JTabbedPane q = new JTabbedPane();
		JPanel x = new JPanel(){
			@Override
			public void paint(){
				
			}
		};
		f.setDefaultCloseOperation(3);
		f.add(q);
		f.setVisible(true);
		q.addTab("?", new JPanel());
	}
}
