package org.embergraph.htree;

import org.embergraph.btree.NodeSerializer;
import org.embergraph.btree.data.ILeafData;
import org.embergraph.htree.data.IDirectoryData;

/**
 * Factory for mutable nodes and leaves used by the {@link NodeSerializer}.
 */
class NodeFactory implements INodeFactory {

	public static final INodeFactory INSTANCE = new NodeFactory();

	private NodeFactory() {
	}

	public DirectoryPage allocNode(final AbstractHTree btree, final long addr,
			final IDirectoryData data) {

		return new DirectoryPage((HTree) btree, addr, data);

	}

	public BucketPage allocLeaf(final AbstractHTree btree, final long addr,
			final ILeafData data) {

		return new BucketPage((HTree) btree, addr, data);

	}

}
