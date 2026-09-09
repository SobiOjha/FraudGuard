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

&#x20;      |

&#x20;      v

Three-Eye Analysis

&#x20;      |

&#x20;      v

Behavioral Risk Scoring

&#x20;      |

&#x20;      v

Risk Classification

&#x20;      |

&#x20;      v

Explainable Fraud Decision

&#x20;      |

&#x20;      v

APPROVE / FLAG / BLOCK

&#x20;      |

&#x20;      v

PostgreSQL

&#x20;      |

&#x20;      v

Dashboard

