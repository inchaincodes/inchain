package org.inchain.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.inchain.crypto.Sha256Hash;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.junit.Test;

public class SHA256Test {

	@Test
	public void testTree() {
		String tx1 = "8c14f0db3df150123e6f3dbbf30f8b955a8249b62ac1d1ff16284aefa3d06d87";
		String tx2 = "fff2525b8931402dd09222c50775608f75787bd2b87e56995a7bdd30f79702c4";
		String tx3 = "6359f0868171b1d194cbee1af2f16ea598ae8fad666d9b012c8ed2b79a236ec4";
		String tx4 = "e9a66845e05d5abc0ad04ec80f774a7e585c6e8db975962d069a522137b80c1d";
		
		String mark = "f3e94742aca4b5ef85488dc37c06c3282295ffec960994b2c0d5ac2a25a95766";
		
		byte[] h1 = Sha256Hash.wrap(Hex.decode(tx1)).getBytes();
		byte[] h2 = Sha256Hash.wrap(Hex.decode(tx2)).getBytes();
		byte[] h3 = Sha256Hash.wrap(Hex.decode(tx3)).getBytes();
		byte[] h4 = Sha256Hash.wrap(Hex.decode(tx4)).getBytes();
		
		byte[] t1 = new byte[h1.length+h2.length];
		System.arraycopy(Utils.reverseBytes(h1), 0, t1, 0, h1.length);
		System.arraycopy(Utils.reverseBytes(h2), 0, t1, h1.length, h2.length);
		
		byte[] nt1 = Utils.reverseBytes(Sha256Hash.hashTwice(t1));
		System.out.println(Hex.encode(nt1));
		
		byte[] t2 = new byte[h3.length+h4.length];
		System.arraycopy(h3, 0, t2, 0, h3.length);
		System.arraycopy(h4, 0, t2, h3.length, h4.length);
		
		byte[] nt2 = Sha256Hash.hashTwice(t2);
		
		byte[] t = new byte[nt1.length + nt2.length];
		System.arraycopy(nt1, 0, t, 0, nt1.length);
		System.arraycopy(nt2, 0, t, nt1.length, nt2.length);
		
		String res = Hex.encode(Sha256Hash.hashTwice(t));
		System.out.println(res);
		
		
		ArrayList<byte[]> tree = new ArrayList<byte[]>();
		tree.add(h1);
		tree.add(h2);
		tree.add(h3);
		tree.add(h4);
		
        int levelOffset = 0; // Offset in the list where the currently processed level starts.
        // Step through each level, stopping when we reach the root (levelSize == 1).
        for (int levelSize = 4; levelSize > 1; levelSize = (levelSize + 1) / 2) {
            // For each pair of nodes on that level:
            for (int left = 0; left < levelSize; left += 2) {
                // The right hand node can be the same as the left hand, in the case where we don't have enough
                // transactions.
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = Utils.reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = Utils.reverseBytes(tree.get(levelOffset + right));
                tree.add(Utils.reverseBytes(Sha256Hash.hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
            }
            // Move to the next level.
            levelOffset += levelSize;
        }
        assertEquals(Hex.encode(tree.get(tree.size() - 1)), mark);
		
	}
}
