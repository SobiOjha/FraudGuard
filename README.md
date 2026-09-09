\# Trinetra



Trinetra is an explainable behavioral fraud detection platform designed to identify suspicious financial transactions and provide a clear protection decision.



Instead of relying only on transaction amount, Trinetra evaluates multiple dimensions of a transaction, including transaction details, behavioral patterns, and contextual signals.



\## Overview



Trinetra analyzes transaction data using a rule-based behavioral risk scoring engine.



The system:



\- Evaluates transaction and behavioral indicators

\- Calculates a fraud risk score from 0 to 100

\- Classifies transactions into risk levels

\- Provides reasons behind the detected risk

\- Determines an action based on the selected protection mode

\- Stores transaction analysis in PostgreSQL

\- Displays transaction history and fraud statistics through a React dashboard



\## How It Works



```text

Transaction Input

      |

      v

Three-Eye Analysis

      |

      v

Behavioral Risk Scoring

      |

      v

Risk Classification

      |

      v

Explainable Fraud Decision

      |

      v

APPROVE / FLAG / BLOCK

      |

      v

PostgreSQL

      |

      v

Dashboard

