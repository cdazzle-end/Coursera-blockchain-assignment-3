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
    
    
    
        
    public class BlockNode{
        private int height;
        private Block block;
        private byte[] blockHash;
        private byte[] parentBlockHash;
        private UTXOPool nodeUtxoPool;
        private TransactionPool nodeTxPool;
        
        public BlockNode(Block b){
            block = b;
            blockHash = block.getHash();
            parentBlockHash = block.getPrevBlockHash();
            
            
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
       
       public void setTxPool(TransactionPool t){
           nodeTxPool = t;
       }
       
       public TransactionPool getTxPool(){
           return nodeTxPool;
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
//        TransactionPool genTxPool = new TransactionPool();
        UTXOPool genUtxoPool = new UTXOPool();
        ArrayList<Transaction> genTxs = genesisBlock.getTransactions();
        Transaction coinbaseTx = genesisBlock.getCoinbase();

        //Add genesis coinbase tx to txPool and UTXO pool
//        genTxPool.addTransaction(coinbaseTx);
        for(int i = 0; i < coinbaseTx.numOutputs(); i++){
            UTXO coinbaseUtxo = new UTXO(coinbaseTx.getHash(), i);
            genUtxoPool.addUTXO(coinbaseUtxo, coinbaseTx.getOutput(i));
        }
        
        //add rest of txs to pools
        for(Transaction tx: genTxs){
            if(tx != null){
                for(int i = 0; i < tx.numOutputs(); i++){
                    Transaction.Output output = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    genUtxoPool.addUTXO(utxo, output);
                }
//                genTxPool.addTransaction(tx);
            }
        }
        
        BlockNode genBlockNode = new BlockNode(genesisBlock, 1);
        genBlockNode.setUtxoPool(genUtxoPool);
//        genBlockNode.setTxPool(genTxPool);
        currentBlockNodes.add(genBlockNode);

    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return maxHeightBlock;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return getMaxHeightBlockNode().getNodeUtxoPool();
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
            return false;
        }
        
        
        TxHandler handler = new TxHandler(parentUtxoPool);
        
        //Get array of tx from current block
        Transaction[] blockTxs = block.getTransactions().toArray(new Transaction[block.getTransactions().size()]);
        Transaction[] validatedTxs = handler.handleTxs(blockTxs);
        if(validatedTxs.length != blockTxs.length){
            return false;
        }
        
//        TransactionPool nodeTxPool = new TransactionPool();
        
        
        for(int i = 0; i < validatedTxs.length; i++){
//            nodeTxPool.addTransaction(validatedTxs[i]);
            addTransaction(validatedTxs[i]);
        }
        UTXOPool newUtxoPool = handler.getUTXOPool();
        Transaction blockCoinbase = block.getCoinbase();
        for(int i =0; i <blockCoinbase.numOutputs(); i++){
            UTXO coinbaseUtxo = new UTXO(blockCoinbase.getHash(), i);
            newUtxoPool.addUTXO(coinbaseUtxo, blockCoinbase.getOutput(i));
        }
        
        
        
        BlockNode newBlockNode = new BlockNode(block);
        newBlockNode.setUtxoPool(newUtxoPool);
//        newBlockNode.setTxPool(nodeTxPool);
        newBlockNode.setHeight(parentBlockNode.getHeight() + 1);
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
        ArrayList<BlockNode> removableBlocks = new ArrayList<>();
        if(minimumHeight > 0){
            for(BlockNode b: currentBlockNodes){
                if(b.getHeight() < minimumHeight){
                    removableBlocks.add(b);
                }
            }
        }
        for(BlockNode b: removableBlocks){
            currentBlockNodes.remove(b);
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