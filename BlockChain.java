package blockchain;
// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 

import java.util.ArrayList;
import java.util.Arrays;

// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    public Block maxHeightBlock;
    public UTXOPool maxUtxoPool = new UTXOPool();
    public TransactionPool txPool = new TransactionPool();
    public ArrayList<BlockNode> currentBlockNodes = new ArrayList<>();
    public int maxHeight = 0;
    public TxHandler txHandler;
    
    
        
    public class BlockNode{
        private int height;
        private Block block;
        private byte[] blockHash;
        private byte[] parentBlockHash;
        private UTXOPool nodeUtxoPool;
        
        public BlockNode(Block b){
            block = b;
            blockHash = block.getHash();
            parentBlockHash = block.getPrevBlockHash();
            nodeUtxoPool = getBlockNodeFromHash(parentBlockHash).getNodeUtxoPool();
        }
        
        //placeholder for genesis block node
        public BlockNode(Block genesisBlock, int h){
            block = genesisBlock;
            blockHash = block.getHash();
            height = h;
        }
//        
//        public void setHeight(){
//            if(block.getPrevBlockHash() >){
//                
//            }
//        }
        
        public void setHeight(int h){
            height = h;
        }
        
        public Block getBlock(){
            return block;
        }
        
        public int getHeight(){
            return height;
        }
        
        public byte[] getHash(){
            return blockHash;
        }
        
       public byte[] getParentBlockHash(){
           return parentBlockHash;
       }
       
       public void setUtxoPool(UTXOPool pool){
           nodeUtxoPool = pool;
       }
        
       public UTXOPool getNodeUtxoPool(){
           return nodeUtxoPool;
       }
        
        
    }
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */

    
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        maxHeightBlock = genesisBlock;
        maxHeight = 1;
        
        //first coins from genesis block
        Transaction coinbaseTx = genesisBlock.getCoinbase();
        UTXOPool genUtxoPool = new UTXOPool();
        //Add genesis coinbase tx to UTXO pool
        for(int i = 0; i < coinbaseTx.numOutputs(); i++){
            UTXO coinbaseUtxo = new UTXO(coinbaseTx.getHash(), i);
            genUtxoPool.addUTXO(coinbaseUtxo, coinbaseTx.getOutput(i));
        }
        TxHandler genTxHandler = new TxHandler(genUtxoPool);
        
        BlockNode genBlockNode = new BlockNode(genesisBlock, 1);
        genBlockNode.setUtxoPool(genUtxoPool);
        currentBlockNodes.add(genBlockNode);
        maxUtxoPool = genUtxoPool;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return maxHeightBlock;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return maxUtxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        if(isGenesisBlock(block)){
            return false;
        }
        
        //Get the tx pool from the block were building on top of
        BlockNode parentBlockNode = getBlockNodeFromHash(block.getPrevBlockHash());
        if(!currentBlockNodes.contains(parentBlockNode))
            return false;
        
        UTXOPool parentUtxoPool;
        
        if(parentBlockNode.getNodeUtxoPool() != null){
            parentUtxoPool = parentBlockNode.getNodeUtxoPool();
        } else {
            parentUtxoPool = maxUtxoPool;
        }
        
        
        TxHandler handler = new TxHandler(parentUtxoPool);
        
        //Get array of tx from current block
        Transaction[] blockTxs = block.getTransactions().toArray(new Transaction[block.getTransactions().size()]);
        
        for(int i = 0; i < blockTxs.length; i++){
            if(handler.isValidTx(blockTxs[i])){
                addTransaction(blockTxs[i]);
            } else {
                return false;
            }
        }
        
        handler.handleTxs(blockTxs);
        UTXOPool newUtxoPool = handler.getUTXOPool();
        
        BlockNode newBlockNode = new BlockNode(block);
        newBlockNode.setUtxoPool(newUtxoPool);
        currentBlockNodes.add(newBlockNode);
        
        //remove confirmed txs from txpool
        for(int i = 0; i < blockTxs.length; i++){
            txPool.removeTransaction(blockTxs[i].getHash());
        }
        
        //update max height block
        if(newBlockNode.getHeight() > maxHeight){
            maxHeightBlock = newBlockNode.getBlock();
            maxHeight = newBlockNode.getHeight();
            maxUtxoPool = newBlockNode.getNodeUtxoPool();
        }
        updateBlockNodes();
        return true;
    }
    
    public boolean isGenesisBlock(Block b){
        if(b.getPrevBlockHash() == null){
            return true;
        } else
            return false;
        
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        txPool.addTransaction(tx);
        
    }
    
    
    //Remove older BlockNodes from current array based on height
    public void updateBlockNodes(){
        BlockNode maxHeightBlockNode = getMaxHeightBlockNode();
        int minimumHeight = maxHeightBlockNode.getHeight() - CUT_OFF_AGE;
        if(minimumHeight > 0){
            for(BlockNode b: currentBlockNodes){
                if(b.getHeight() < minimumHeight){
                    currentBlockNodes.remove(b);
                }
            }
        }
    }
    
    //make it easy to get maxHeightBlockNode
    public BlockNode getMaxHeightBlockNode(){
        BlockNode maxHeightBlockNode = getBlockNodeFromHash(getMaxHeightBlock().getHash());
        return maxHeightBlockNode;
    }
    
    //Get BlockNode from current BlockNode array
    public BlockNode getBlockNodeFromHash(byte[] blockHash){
        
        for(BlockNode b: currentBlockNodes){
            if(Arrays.equals(b.getHash(), blockHash)){
                return b;
            } 
        }
        return null;
    }
    
    
}