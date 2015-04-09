This is a reimplementation of the Berkeley Parser. Some of the class files
are copied from the Berkeley parser with possible changes to class
names and contents. The original purpose of the reimplementation was
to develop ideas that cannot be easily implemented on top of the
original Berkeley Parser. The parser has subsequently evolved with
several enhancements to improve parsing performance. Here are some
highlights of the parser:

1. Training is parallelized to support multi-threading;

2. Parsing is parallelized, supports n-best extraction, constrained and unconstrained viterbi decoding, parsing score reporting, and parsing with multiple grammars through a product model;

3. Includes a featured lexical model to: 1) alleviate over-fitting via regularization, 2) handle OOV words using a featured OOV model P(POS\_tag|word), and 3) exploit lexical features for grammar induction using a featured latent lexical model P(latent\_tag|word,POS\_tag).

This parser was used in the following papers:

1. Self-Training PCFG Grammars with Latent Annotations Across Languages, Zhongqiang Huang and Mary Harper, EMNLP 2009

2. Self-training with Products of Latent Variable Grammars, Zhongqiang Huang, Mary Harper, and Slav Petrov, EMNLP 2010

3. Feature-Rich Log-Linear Lexical Model for Latent Variable PCFG Grammars, Zhongqiang Huang and Mary Harper, IJCNLP 2011