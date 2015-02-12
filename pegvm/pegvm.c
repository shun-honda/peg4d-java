#include <stdio.h>
#include <sys/time.h> // gettimeofday
#include <assert.h>
#include <string.h>
#include "libnez.h"
#include "pegvm.h"

#ifdef DHAVE_CONFIG_H
#include "config.h"
#endif

uint64_t timer() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

void nez_ShowUsage(const char *file) {
  // fprintf(stderr, "Usage: %s -f peg_bytecode target_file\n", file);
  fprintf(stderr, "\npegvm <command> optional files\n");
  fprintf(stderr, "  -p <filename> Specify an PEGs grammar bytecode file\n");
  fprintf(stderr, "  -i <filename> Specify an input file\n");
  fprintf(stderr, "  -o <filename> Specify an output file\n");
  fprintf(stderr, "  -t <type>     Specify an output type\n");
  fprintf(stderr, "  -h            Display this help and exit\n\n");
  exit(EXIT_FAILURE);
}

void nez_PrintErrorInfo(const char *errmsg) {
  fprintf(stderr, "%s\n", errmsg);
  exit(EXIT_FAILURE);
}

static char *loadFile(const char *filename, size_t *length) {
  size_t len = 0;
  FILE *fp = fopen(filename, "rb");
  char *source;
  if (!fp) {
    return NULL;
  }
  fseek(fp, 0, SEEK_END);
  len = (size_t)ftell(fp);
  fseek(fp, 0, SEEK_SET);
  source = (char *)malloc(len + 1);
  if (len != fread(source, 1, len, fp)) {
    fprintf(stderr, "fread error\n");
    exit(EXIT_FAILURE);
  }
  source[len] = '\0';
  fclose(fp);
  *length = len;
  return source;
}

static const char *get_opname(uint8_t opcode) {
  switch (opcode) {
#define OP_DUMPCASE(OP) \
  case PEGVM_OP_##OP:   \
    return "" #OP;
    PEGVM_OP_EACH(OP_DUMPCASE);
  default:
    assert(0 && "UNREACHABLE");
    break;
#undef OP_DUMPCASE
  }
  return "";
}

static void dump_PegVMInstructions(Instruction *inst, uint64_t size) {
  uint64_t i;
  int j;
  for (i = 0; i < size; i++) {
    j = 0;
    fprintf(stderr, "[%llu] %s ", i, get_opname(inst[i].opcode));
    if (inst[i].ndata) {
      switch (inst->opcode) {
#define OP_DUMPCASE(OP) case PEGVM_OP_##OP:
      default:
        // fprintf(stderr, "%d ", inst[i].jump);
        break;
      }
    }
    if (inst[i].chardata) {
      fprintf(stderr, "%s", inst[i].chardata);
    }
    fprintf(stderr, "\n");
  }
}

static void dump_byteCodeInfo(byteCodeInfo *info) {
  fprintf(stderr, "ByteCodeVersion:%u.%u\n", info->version0, info->version1);
  fprintf(stderr, "PEGFile:%s\n", info->filename);
  fprintf(stderr, "LengthOfByteCode:%llu\n", info->bytecode_length);
  fprintf(stderr, "\n");
}

static uint32_t read32(char *inputs, byteCodeInfo *info) {
  uint32_t value = 0;
  value = (uint8_t)inputs[info->pos++];
  value = (value) | ((uint8_t)inputs[info->pos++] << 8);
  value = (value) | ((uint8_t)inputs[info->pos++] << 16);
  value = (value) | ((uint8_t)inputs[info->pos++] << 24);
  return value;
}

static uint64_t read64(char *inputs, byteCodeInfo *info) {
  uint64_t value1 = read32(inputs, info);
  uint64_t value2 = read32(inputs, info);
  return value2 << 32 | value1;
}

PegVMInstruction *nez_LoadMachineCode(ParsingContext context,
                                      PegVMInstruction *inst,
                                      const char *fileName,
                                      const char *nonTerminalName) {
  size_t len;
  char *buf = loadFile(fileName, &len);
  int j = 0;
  byteCodeInfo info;
  info.pos = 0;

  info.version0 = buf[info.pos++];
  info.version1 = buf[info.pos++];
  info.filename_length = read32(buf, &info);
  info.filename = malloc(sizeof(uint8_t) * info.filename_length + 1);
  for (uint32_t i = 0; i < info.filename_length; i++) {
    info.filename[i] = buf[info.pos++];
  }
  info.filename[info.filename_length] = 0;
  info.pool_size_info = read32(buf, &info);

  int ruleSize = read32(buf, &info);
  char **ruleTable = (char **)malloc(sizeof(char *) * ruleSize);
  for (int i = 0; i < ruleSize; i++) {
    int ruleNameLen = read32(buf, &info);
    ruleTable[i] = (char *)malloc(ruleNameLen);
    for (int j = 0; j < ruleNameLen; j++) {
      ruleTable[i][j] = buf[info.pos++];
    }
    long index = read64(buf, &info);
    if (nonTerminalName != NULL) {
      if (!strcmp(ruleTable[i], nonTerminalName)) {
        context->startPoint = index;
      }
    }
  }

  if (context->startPoint == 0) {
    context->startPoint = 1;
  }

  info.bytecode_length = read64(buf, &info);

  // dump byte code infomation
  dump_byteCodeInfo(&info);

  // free bytecode info
  free(info.filename);
  for (int i = 0; i < ruleSize; i++) {
    free(ruleTable[i]);
  }

  // memset(inst, 0, sizeof(*inst) * info.bytecode_length);
  inst = malloc(sizeof(*inst) * info.bytecode_length);

  for (uint64_t i = 0; i < info.bytecode_length; i++) {
    int code_length;
    inst[i].opcode = buf[info.pos++];
    code_length = (uint8_t)buf[info.pos++];
    code_length = (code_length) | ((uint8_t)buf[info.pos++] << 8);
    if (code_length != 0) {
      if (inst[i].opcode == PEGVM_OP_CHAR ||
          inst[i].opcode == PEGVM_OP_NOTCHAR ||
          inst[i].opcode == PEGVM_OP_OPTIONALCHAR) {
        inst[i].chardata = (char *)malloc(sizeof(char));
        inst[i].chardata[0] = read32(buf, &info);
      } else if (code_length == 1) {
        inst[i].ndata = malloc(sizeof(int));
        inst[i].ndata[0] = read32(buf, &info);
      } else if (inst[i].opcode == PEGVM_OP_MAPPEDCHOICE ||
                 inst[i].opcode == PEGVM_OP_CHARCLASS ||
                 inst[i].opcode == PEGVM_OP_NOTCHARCLASS ||
                 inst[i].opcode == PEGVM_OP_OPTIONALCHARCLASS ||
                 inst[i].opcode == PEGVM_OP_ZEROMORECHARCLASS) {
        inst[i].ndata = malloc(sizeof(int) * code_length);
        while (j < code_length) {
          inst[i].ndata[j] = read32(buf, &info);
          j++;
        }
      } else if (inst[i].opcode == PEGVM_OP_SCAN) {
        inst[i].ndata = malloc(sizeof(int) * 2);
        inst[i].ndata[0] = read32(buf, &info);
        inst[i].ndata[1] = read32(buf, &info);
      } else {
        inst[i].ndata = malloc(sizeof(int));
        inst[i].ndata[0] = code_length;
        inst[i].chardata = malloc(sizeof(int) * code_length);
        while (j < code_length) {
          inst[i].chardata[j] = read32(buf, &info);
          j++;
        }
      }
    }
    j = 0;
    inst[i].jump = inst + read32(buf, &info);
    code_length = buf[info.pos++];
    if (code_length != 0) {
      inst[i].chardata = malloc(sizeof(char) * code_length + 1);
      while (j < code_length) {
        inst[i].chardata[j] = buf[info.pos++];
        j++;
      }
      inst[i].chardata[code_length] = 0;
    }
    j = 0;
  }

  if (PEGVM_DEBUG) {
    dump_PegVMInstructions(inst, info.bytecode_length);
  }

  context->bytecode_length = info.bytecode_length;
  context->pool_size = info.pool_size_info;

  //    for (long i = 0; i < context->bytecode_length; i++) {
  //        if((inst+i)->opcode < PEGVM_OP_ANDSTRING) {
  //            (inst+i)->jump_inst = inst+(inst+i)->jump;
  //        }
  //    }

  // free(buf);
  return inst;
}

void nez_DisposeInstruction(Instruction *inst, long length) {
  for (long i = 0; i < length; i++) {
    if (inst[i].ndata != NULL) {
      free(inst[i].ndata);
      inst[i].ndata = NULL;
    }
    if (inst[i].chardata != NULL) {
      free(inst[i].chardata);
      inst[i].chardata = NULL;
    }
  }
  free(inst);
  inst = NULL;
}

ParsingContext nez_CreateParsingContext(ParsingContext ctx,
                                        const char *filename) {
  ctx = (ParsingContext)malloc(sizeof(struct ParsingContext));
  ctx->pos = ctx->input_size = 0;
  ctx->startPoint = 0;
  ctx->mpool = (MemoryPool)malloc(sizeof(struct MemoryPool));
  ctx->inputs = loadFile(filename, &ctx->input_size);
  ctx->stackedSymbolTable =
      (SymbolTableEntry)malloc(sizeof(struct SymbolTableEntry) * 256);
  // P4D_setObject(ctx, &ctx->left, P4D_newObject(ctx, ctx->pos));
  ctx->stack_pointer_base =
      (long *)malloc(sizeof(long) * PARSING_CONTEXT_MAX_STACK_LENGTH);
  ctx->object_stack_pointer_base = (ParsingObject *)malloc(
      sizeof(ParsingObject) * PARSING_CONTEXT_MAX_STACK_LENGTH);
  ctx->call_stack_pointer_base = (Instruction **)malloc(
      sizeof(Instruction *) * PARSING_CONTEXT_MAX_STACK_LENGTH);
  ctx->stack_pointer = &ctx->stack_pointer_base[0];
  ctx->object_stack_pointer = &ctx->object_stack_pointer_base[0];
  ctx->call_stack_pointer = &ctx->call_stack_pointer_base[0];
  return ctx;
}

void nez_DisposeObject(ParsingObject *pego) {
  if (pego[0] != NULL) {
    if (pego[0]->child_size != 0) {
      for (int i = 0; i < pego[0]->child_size; i++) {
        nez_DisposeObject(&pego[0]->child[i]);
      }
      free(pego[0]->child);
      pego[0]->child = NULL;
    }
    // free(pego[0]);
    // pego[0] = NULL;
  }
}

void nez_DisposeParsingContext(ParsingContext ctx) {
  free(ctx->inputs);
  ctx->inputs = NULL;
  free(ctx->mpool);
  ctx->mpool = NULL;
  free(ctx->stackedSymbolTable);
  ctx->stackedSymbolTable = NULL;
  free(ctx->call_stack_pointer_base);
  ctx->call_stack_pointer_base = NULL;
  free(ctx->stack_pointer_base);
  ctx->stack_pointer_base = NULL;
  free(ctx->object_stack_pointer_base);
  ctx->object_stack_pointer_base = NULL;
  free(ctx);
  ctx = NULL;
  // dispose_pego(&ctx->unusedObject);
}

// static inline int ParserContext_IsFailure(ParsingContext context)
//{
//    return context->left == NULL;
//}
//
// static void ParserContext_RecordFailurePos(ParsingContext context, size_t
// consumed)
//{
//    context->left = NULL;
//    context->pos -= consumed;
//}

// #define INC_SP(N) (context->stack_pointer += (N))
// #define DEC_SP(N) (context->stack_pointer -= (N))
static inline long INC_SP(ParsingContext context, int N) {
  context->stack_pointer += (N);
#if PEGVM_DEBUG
  assert(context->stack_pointer >= context->stack_pointer_base &&
         context->stack_pointer <
             &context->stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
#endif
  return *context->stack_pointer;
}

static inline long DEC_SP(ParsingContext context, int N) {
  context->stack_pointer -= N;
#if PEGVM_DEBUG
  assert(context->stack_pointer >= context->stack_pointer_base &&
         context->stack_pointer <
             &context->stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
#endif
  return *context->stack_pointer;
}

static inline ParsingObject INC_OSP(ParsingContext context, int N) {
  context->object_stack_pointer += (N);
#if PEGVM_DEBUG
  assert(context->object_stack_pointer >= context->object_stack_pointer_base &&
         context->object_stack_pointer <
             &context->object_stack_pointer_base
                  [PARSING_CONTEXT_MAX_STACK_LENGTH]);
#endif
  return *context->object_stack_pointer;
}

static inline ParsingObject DEC_OSP(ParsingContext context, int N) {
  context->object_stack_pointer -= N;
#if PEGVM_DEBUG
  assert(context->object_stack_pointer >= context->object_stack_pointer_base &&
         context->object_stack_pointer <
             &context->object_stack_pointer_base
                  [PARSING_CONTEXT_MAX_STACK_LENGTH]);
#endif
  return *context->object_stack_pointer;
}

static inline void PUSH_IP(ParsingContext context, Instruction *pc) {
  *context->call_stack_pointer++ = pc;
#if PEGVM_DEBUG
  assert(
      context->call_stack_pointer >= context->call_stack_pointer_base &&
      context->call_stack_pointer <
          &context->call_stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
#endif
}

static inline Instruction **POP_IP(ParsingContext context) {
  --context->call_stack_pointer;
  assert(
      context->call_stack_pointer >= context->call_stack_pointer_base &&
      context->call_stack_pointer <
          &context->call_stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
  return context->call_stack_pointer;
}

#define SP_TOP(INST) (*context->stack_pointer)
#define PUSH_SP(INST) (*context->stack_pointer = (INST), INC_SP(context, 1))
#define POP_SP(INST) (DEC_SP(context, 1))
#define PUSH_OSP(INST) \
  (*context->object_stack_pointer = (INST), INC_OSP(context, 1))
#define POP_OSP(INST) (DEC_OSP(context, 1))
//#define TOP_SP() (*context->stack_pointer)
#define JUMP       \
  pc = (pc)->jump; \
  goto *(pc)->ptr;
#define RET              \
  pc = *POP_IP(context); \
  goto *(pc)->ptr;

#if PEGVM_PROFILE
static uint64_t count[PEGVM_OP_MAX];
static uint64_t conbination_count[PEGVM_OP_MAX][PEGVM_OP_MAX];
static uint64_t count_all;
static uint64_t rule_count[100];
#define DISPATCH_NEXT                          \
  int first = (int)pc->opcode;                 \
  ++pc;                                        \
  conbination_count[first][(int)pc->opcode]++; \
  goto *(pc)->ptr;
#define OP(OP)                            \
  PEGVM_OP_##OP : count[PEGVM_OP_##OP]++; \
  count_all++;
#else
#define OP(OP) PEGVM_OP_##OP:
#define DISPATCH_NEXT \
  ++pc;               \
  goto *(pc)->ptr;
#endif

static const char *get_json_rule(uint8_t json_rule) {
  switch (json_rule) {
#define json_CASE(RULE)           \
  case PEGVM_PROFILE_json_##RULE: \
    return "" #RULE;
    PEGVM_PROFILE_json_EACH(json_CASE);
  default:
    assert(0 && "UNREACHABLE");
    break;
#undef json_CASE
  }
  return "";
}

static const char *get_xml_rule(uint8_t xml_rule) {
  switch (xml_rule) {
#define xml_CASE(RULE)           \
  case PEGVM_PROFILE_xml_##RULE: \
    return "" #RULE;
    PEGVM_PROFILE_xml_EACH(xml_CASE);
  default:
    assert(0 && "UNREACHABLE");
    break;
#undef xml_CASE
  }
  return "";
}

static const char *get_c99_rule(uint8_t c99_rule) {
  switch (c99_rule) {
#define c99_CASE(RULE)           \
  case PEGVM_PROFILE_c99_##RULE: \
    return "" #RULE;
    PEGVM_PROFILE_c99_EACH(c99_CASE);
  default:
    assert(0 && "UNREACHABLE");
    break;
#undef c99_CASE
  }
  return "";
}

void nez_VM_PrintProfile(const char *file_type) {
#if PEGVM_PROFILE
  fprintf(stderr, "\ninstruction count \n");
  for (int i = 0; i < PEGVM_PROFILE_MAX; i++) {
    fprintf(stderr, "%llu %s\n", count[i], get_opname(i));
    // fprintf(stderr, "%s: %llu (%0.2f%%)\n", get_opname(i), count[i],
    // (double)count[i]*100/(double)count_all);
  }
  FILE *file;
  file = fopen("pegvm_profile.csv", "w");
  if (file == NULL) {
    assert(0 && "can not open file");
  }
  fprintf(file, ",");
  for (int i = 0; i < PEGVM_PROFILE_MAX; i++) {
    fprintf(file, "%s", get_opname(i));
    if (i != PEGVM_PROFILE_MAX - 1) {
      fprintf(file, ",");
    }
  }
  for (int i = 0; i < PEGVM_PROFILE_MAX; i++) {
    fprintf(file, "%s,", get_opname(i));
    for (int j = 0; j < PEGVM_PROFILE_MAX; j++) {
      fprintf(file, "%llu", conbination_count[i][j]);
      if (j != PEGVM_PROFILE_MAX - 1) {
        fprintf(file, ",");
      }
    }
    fprintf(file, "\n");
  }
  fclose(file);
  if (file_type) {
    fprintf(stderr, "\nrule_count\n");
    if (!strcmp(file_type, "json")) {
      for (int i = 0; i < PEGVM_json_RULE_MAX; i++) {
        fprintf(stderr, "%llu %s\n", rule_count[i], get_json_rule(i));
      }
    } else if (!strcmp(file_type, "xml")) {
      for (int i = 0; i < PEGVM_xml_RULE_MAX; i++) {
        fprintf(stderr, "%llu %s\n", rule_count[i], get_xml_rule(i));
      }
    } else if (!strcmp(file_type, "c99")) {
      for (int i = 0; i < PEGVM_c99_RULE_MAX; i++) {
        fprintf(stderr, "%llu %s\n", rule_count[i], get_c99_rule(i));
      }
    }
  }
#endif
}

ParsingObject nez_Parse(ParsingContext context, Instruction *inst) {
  if (nez_VM_Execute(context, inst)) {
    nez_PrintErrorInfo("parse error");
  }
  dump_pego(&context->left, context->inputs, 0);
  return context->left;
}

void nez_ParseStat(ParsingContext context, Instruction *inst) {
  for (int i = 0; i < 5; i++) {
    uint64_t start, end;
    MemoryPool_Reset(context->mpool);
    start = timer();
    if (nez_VM_Execute(context, inst)) {
      nez_PrintErrorInfo("parse error");
    }
    end = timer();
    fprintf(stderr, "ErapsedTime: %llu msec\n", end - start);
    nez_DisposeObject(&context->left);
    context->pos = 0;
  }
}

void nez_Match(ParsingContext context, Instruction *inst) {
  if (nez_VM_Execute(context, inst)) {
    nez_PrintErrorInfo("parse error");
  }
  fprintf(stdout, "match\n\n");
  nez_DisposeObject(&context->left);
}

Instruction *nez_VM_Prepare(ParsingContext context, Instruction *inst) {
  long i;
  const void **table = (const void **)nez_VM_Execute(context, NULL);
  for (i = 0; i < context->bytecode_length; i++) {
    (inst + i)->ptr = table[(inst + i)->opcode];
  }
  return inst;
}

long nez_VM_Execute(ParsingContext context, Instruction *inst) {
  static const void *table[] = {
#define DEFINE_TABLE(NAME) &&PEGVM_OP_##NAME,
    PEGVM_OP_EACH(DEFINE_TABLE)
#undef DEFINE_TABLE
  };

  if (inst == NULL) {
    return (long)table;
  }

  int failflag = 0;
  ParsingObject left = context->left;
  long pos = context->pos;
  Instruction *pc = inst + context->startPoint;
  const char *inputs = context->inputs;
  MemoryPool pool = context->mpool;

  PUSH_IP(context, inst);
  P4D_setObject(context, &left, P4D_newObject(context, context->pos, pool));

  goto *(pc)->ptr;

  OP(EXIT) {
    P4D_commitLog(context, 0, left, pool);
    context->left = left;
    context->pos = pos;
    return failflag;
  }
  OP(JUMP) { JUMP; }
  OP(CALL) {
    PUSH_IP(context, pc + 1);
#if PEGVM_PROFILE
    rule_count[pc->ndata[0]]++;
#endif
    JUMP;
  }
  OP(RET) { RET; }
  OP(CONDBRANCH) {
    if (failflag == *pc->ndata) {
      JUMP;
    }
    DISPATCH_NEXT;
  }
  OP(CHAR) {
    if (pc->chardata[0] == inputs[pos]) {
      pos++;
      DISPATCH_NEXT;
    }
    failflag = 1;
    JUMP;
  }
  OP(CHARCLASS) {
    if (pc->ndata[(int)inputs[pos]]) {
      pos++;
      DISPATCH_NEXT;
    }
    failflag = 1;
    JUMP;
  }
  OP(STRING) {
    char *p = pc->chardata;
    int len = *pc->ndata;
    char *pend = pc->chardata + len;
    while (p < pend) {
      if (inputs[pos] != *p++) {
        failflag = 1;
        JUMP;
      }
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(ANY) {
    if (inputs[pos++] == 0) {
      pos--;
      failflag = 1;
      JUMP;
    }
    DISPATCH_NEXT;
  }
  OP(PUSHo) {
    ParsingObject po = left;
    left->refc++;
    PUSH_OSP(po);
    PUSH_SP(P4D_markLogStack(context));
    DISPATCH_NEXT;
  }
  OP(PUSHp) {
    PUSH_SP(pos);
    DISPATCH_NEXT;
  }
  OP(POPo) {
    ParsingObject po = POP_OSP();
    P4D_setObject(context, &po, NULL);
    DISPATCH_NEXT;
  }
  OP(POPp) {
    POP_SP();
    DISPATCH_NEXT;
  }
  OP(GETp) {
    pos = SP_TOP();
    DISPATCH_NEXT;
  }
  OP(STOREo) {
    ParsingObject po = POP_OSP();
    P4D_setObject(context, &left, po);
    DISPATCH_NEXT;
  }
  OP(STOREp) {
    pos = POP_SP();
    DISPATCH_NEXT;
  }
  OP(STOREflag) {
    failflag = pc->ndata[0];
    DISPATCH_NEXT;
  }
  OP(STOREp_flag) {
    pos = POP_SP();
    failflag = pc->ndata[0];
    DISPATCH_NEXT;
  }
  OP(STOREendp) {
    left->end_pos = pos;
    POP_SP();
    DISPATCH_NEXT;
  }
  OP(NEW) {
    PUSH_SP(P4D_markLogStack(context));
    P4D_setObject(context, &left, P4D_newObject(context, pos, pool));
    // PUSH_SP(P4D_markLogStack(context));
    DISPATCH_NEXT;
  }
  OP(LEFTJOIN) {
    ParsingObject po = NULL;
    P4D_setObject(context, &po, left);
    P4D_setObject(context, &left, P4D_newObject(context, pos, pool));
    P4D_lazyJoin(context, po, pool);
    P4D_lazyLink(context, left, *(pc)->ndata, po, pool);
    DISPATCH_NEXT;
  }
  OP(COMMITLINK) {
    P4D_commitLog(context, (int)POP_SP(), left, pool);
    ParsingObject parent = (ParsingObject)POP_OSP();
    P4D_lazyLink(context, parent, *(pc)->ndata, left, pool);
    P4D_setObject(context, &left, parent);
    DISPATCH_NEXT;
  }
  OP(ABORT) {
    P4D_abortLog(context, (int)POP_SP());
    DISPATCH_NEXT;
  }
  OP(TAG) {
    left->tag = (pc)->chardata;
    DISPATCH_NEXT;
  }
  OP(VALUE) {
    left->value = (pc)->chardata;
    DISPATCH_NEXT;
  }
  OP(MAPPEDCHOICE) {
    pc = inst + (pc)->ndata[(int)inputs[pos]];
    goto *(pc)->ptr;
  }
  OP(SCAN) {
    long start = POP_SP();
    long len = pos - start;
    char value[len];
    int j = 0;
    for (long i = start; i < pos; i++) {
      value[j] = inputs[i];
      j++;
    }
    if (pc->ndata[0] == 16) {
      long num = strtol(value, NULL, 16);
      context->repeat_table[pc->ndata[1]] = (int)num;
      DISPATCH_NEXT;
    }
    context->repeat_table[pc->ndata[1]] = atoi(value);
    DISPATCH_NEXT;
  }
  OP(IFREPEATEND) {
    if (context->repeat_table[pc->ndata[0]] == 0) {
      DISPATCH_NEXT;
    }
    JUMP;
  }
  OP(DEF) {
    long start = POP_SP();
    int len = (int)(pos - start);
    char *value = malloc(len);
    int j = 0;
    for (long i = start; i < pos; i++) {
      value[j] = inputs[i];
      j++;
    }
    pushSymbolTable(context, pc->ndata[0], len, value);
    DISPATCH_NEXT;
  }
  OP(IS) {
    failflag = matchSymbolTableTop(context, &pos, pc->ndata[0]);
    DISPATCH_NEXT;
  }
  OP(ISA) {
    failflag = matchSymbolTable(context, &pos, pc->ndata[0]);
    DISPATCH_NEXT;
  }
  OP(BLOCKstart) {
    long len;
    PUSH_SP(context->stateValue);
    char *value = getIndentText(context, inputs, pos, &len);
    PUSH_SP(pushSymbolTable(context, pc->ndata[0], (int)len, value));
    DISPATCH_NEXT;
  }
  OP(BLOCKend) {
    popSymbolTable(context, (int)POP_SP());
    context->stateValue = (int)POP_SP();
    DISPATCH_NEXT;
  }
  OP(INDENT) {
    matchSymbolTableTop(context, &pos, pc->ndata[0]);
    DISPATCH_NEXT;
  }
  OP(NOTCHAR) {
    if (inputs[pos] == *(pc)->chardata) {
      failflag = 1;
      JUMP;
    }
    DISPATCH_NEXT;
  }
  OP(NOTCHARCLASS) {
    if (pc->ndata[(int)inputs[pos]]) {
      failflag = 1;
      JUMP;
    }
    DISPATCH_NEXT;
  }
  OP(NOTSTRING) {
    char *p = pc->chardata;
    int len = *pc->ndata;
    long backtrack_pos = pos;
    char *pend = pc->chardata + len;
    while (p < pend) {
      if (inputs[pos] != *p++) {
        pos = backtrack_pos;
        DISPATCH_NEXT;
      }
      pos++;
    }
    pos = backtrack_pos;
    failflag = 1;
    JUMP;
  }
  OP(OPTIONALCHAR) {
    if (inputs[pos] == *(pc)->chardata) {
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALCHARCLASS) {
    if ((pc)->ndata[(int)inputs[pos]]) {
      pos++;
      DISPATCH_NEXT;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALSTRING) {
    char *p = pc->chardata;
    int len = *pc->ndata;
    long backtrack_pos = pos;
    char *pend = pc->chardata + len;
    while (p < pend) {
      if (inputs[pos] != *p++) {
        pos = backtrack_pos;
        DISPATCH_NEXT;
      }
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(ZEROMORECHARCLASS) {
    while (1) {
      if (!(pc)->ndata[(int)inputs[pos]]) {
        break;
      }
      pos++;
    }
    DISPATCH_NEXT;
  }

  return failflag;
}
