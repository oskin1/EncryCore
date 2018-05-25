# UTXO security without maintaining a full set of outputs

## Abstract

This paper is devoted to the study of UTXO maintaining in massively replicated open blockchain systems. 
In such systems, like Bitcoin, a snapshot of current state required for the validation of transactions is being held in 
the memory, which eventually becomes a scarce resource. Uncontrolled state growth can lead to security issues. We propose 
a modification of UTXO model and also transaction validation algorithm.

## UOHS (Unspent Output Hashes Set)

The idea is to store in UTXO only hashes of unspent outputs rather than their full versions. Meanwhile, the transaction 
trying to spend some output should contain its full version for validation purposes. This allows to significantly decrease 
the amount of data required to be stored in UTXO which reside in expensive random access memory (RAM). Tha main drawback 
of such approach is an increase of network load.

## Transaction validation algorithm

Supposing transaction contains full inputs we have the following validation algorithm:

0. Check semantic validity of transaction
1. For each input in transaction:
    1. Check semantic validity of input
    2. Try to unlock the box, providing appropriate context and proof
    3. Check whether its hash is presented in UOHS
4. Make sure inputs.sum >= outputs.sum

## Research

To find out whether the security overhead achieved by such approach overweights its drawbacks (network load increase), lets
take a look at the structure of the most basic type of transaction:

Payment transaction example (Length in bytes by different components):

    Transaction length:    264
    Inputs lengths (id):   Vector(32)
    Outputs lengths:       Vector(57, 57, 20)
    Outputs lengths total: 134

Average transaction size ~ 265 bytes (250 bytes in Bitcoin)
Average input size       ~ 46 bytes

Current transaction structure:

    senderPubKey:   32 bytes
    signature:      64 bytes
    timestamp:       8 bytes
    fee:             8 bytes
    inputs:         32 bytes / unit
    outputs:       ~46 bytes / unit

Storing full versions of inputs in transaction will lead to increase of its size. Now it contains only ids of inputs which are
32 bytes length, current transaction size is ~265 bytes average. Supposing that average input size is 46 bytes and average quantity
is 1, the increase of total transaction length will be about ~ 5.2%, the decrease of state size ~ 31% (in case of minimal box size).
