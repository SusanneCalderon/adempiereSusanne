/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.core.domains.models.X_C_ProjectIssue;
import org.adempiere.core.domains.models.X_C_ProjectIssueMA;
import org.adempiere.engine.IDocumentLine;
import org.adempiere.exceptions.PeriodClosedException;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * 	Project Issue Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProjectIssue.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class MProjectIssue extends X_C_ProjectIssue implements IDocumentLine, DocAction
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4714411434615096132L;
	/**	Process Message 			*/
	private String processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean justPrepared = false;

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_ProjectIssue_ID id
	 *	@param trxName transaction
	 */
	public MProjectIssue (Properties ctx, int C_ProjectIssue_ID, String trxName)
	{
		super (ctx, C_ProjectIssue_ID, trxName);
		if (C_ProjectIssue_ID == 0)
		{
		//	setC_Project_ID (0);
		//	setLine (0);
		//	setM_Locator_ID (0);
		//	setM_Product_ID (0);
		//	setMovementDate (new Timestamp(System.currentTimeMillis()));
			setMovementQty (Env.ZERO);
			setPosted (false);
			setProcessed (false);
		}
	}	//	MProjectIssue

	/**
	 * 	Load Constructor
	 * 	@param ctx context
	 * 	@param rs result set
	 *	@param trxName transaction
	 */
	public MProjectIssue (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProjectIssue

	/**
	 * 	New Parent Constructor
	 *	@param project parent
	 */
	public MProjectIssue (MProject project)
	{
		this (project.getCtx(), 0, project.get_TrxName());
		setClientOrg(project.getAD_Client_ID(), project.getAD_Org_ID());
		setC_Project_ID (project.getC_Project_ID());	//	Parent
		setLine (getNextLine());
		m_parent = project;
		//
	//	setM_Locator_ID (0);
	//	setM_Product_ID (0);
		//
		setMovementDate (new Timestamp(System.currentTimeMillis()));
		setMovementQty (Env.ZERO);
		setPosted (false);
		setProcessed (false);
	}	//	MProjectIssue

	/**	Parent				*/
	private MProject	m_parent = null;
	
	/**
	 *	Get the next Line No
	 * 	@return next line no
	 */
	private int getNextLine()
	{
		return DB.getSQLValue(get_TrxName(), 
			"SELECT COALESCE(MAX(Line),0)+10 FROM C_ProjectIssue WHERE C_Project_ID=?", getC_Project_ID());
	}	//	getLineFromProject

	/**
	 * 	Set Mandatory Values
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param MovementQty qty
	 */
	public void setMandatory (int M_Locator_ID, int M_Product_ID, BigDecimal MovementQty)
	{
		setM_Locator_ID (M_Locator_ID);
		setM_Product_ID (M_Product_ID);
		setMovementQty (MovementQty);
	}	//	setMandatory

	/**
	 * 	Get Parent
	 *	@return project
	 */
	public MProject getParent()
	{
		if (m_parent == null && getC_Project_ID() != 0)
			m_parent = new MProject (getCtx(), getC_Project_ID(), get_TrxName());
		return m_parent;
	}	//	getParent
	
	/**************************************************************************
	 * 	Process Issue
	 *	@return true if processed
	 */
	public boolean process()
	{
		if (!save())
			return false;
		if (getM_Product_ID() == 0)
		{
			log.log(Level.SEVERE, "No Product");
			return false;
		}

		MProduct product = MProduct.get (getCtx(), getM_Product_ID());

		//	If not a stocked Item nothing to do
		if (!product.isStocked())
		{
			setProcessed(true);
			return save();
		}

		/** @todo Transaction */

		//	**	Create Material Transactions **
		MTransaction mTrx = new MTransaction (getCtx(), getAD_Org_ID(), 
			MTransaction.MOVEMENTTYPE_WorkOrder_,
			getM_Locator_ID(), getM_Product_ID(), getM_AttributeSetInstance_ID(),
			getMovementQty().negate(), getMovementDate(), get_TrxName());
		mTrx.setC_ProjectIssue_ID(getC_ProjectIssue_ID());
		//
		MLocator loc = MLocator.get(getCtx(), getM_Locator_ID());
		if (MStorage.add(getCtx(), loc.getM_Warehouse_ID(), getM_Locator_ID(), 
				getM_Product_ID(), getM_AttributeSetInstance_ID(), getM_AttributeSetInstance_ID(),
				getMovementQty().negate(), null, null, get_TrxName()))
		{
			if (mTrx.save(get_TrxName()))
			{
				setProcessed (true);
				if (save())
					return true;
				else
					log.log(Level.SEVERE, "Issue not saved");		//	requires trx !!
			}
			else
				log.log(Level.SEVERE, "Transaction not saved");	//	requires trx !!
		}
		else
			log.log(Level.SEVERE, "Storage not updated");			//	OK
		//
		return false;
	}	//	process

	@Override
	public int getC_DocType_ID() {
		return MDocType.getDocType(MDocType.DOCBASETYPE_ProjectIssue, getAD_Org_ID());
	}

	@Override
	public int getM_LocatorTo_ID() {
		return 0;
	}

	@Override
	public int getM_AttributeSetInstanceTo_ID() {
		return 0;
	}

	@Override
	public Timestamp getDateAcct() {
		return getMovementDate();
	}

	@Override
	public boolean isSOTrx() {
		return false;
	}

	@Override
	public int getReversalLine_ID() {
		return getReversal_ID();
	}

	@Override
	public BigDecimal getPriceActual() {
		return BigDecimal.ZERO;
	}

	@Override
	public IDocumentLine getReversalDocumentLine() {

		return (IDocumentLine) getReversal();
	}

	@Override
	public BigDecimal getPriceActualCurrency() {
		return BigDecimal.ZERO;
	}

	@Override
	public int getC_Currency_ID ()
	{
		MClient client  = MClient.get(getCtx());
		return client.getC_Currency_ID();
	}

	@Override
	public int getC_ConversionType_ID()
	{
		return  MConversionType.getDefault(getAD_Client_ID());
	}

	@Override
	public boolean isReversalParent() {
		return getC_ProjectIssue_ID() < getReversalLine_ID();
	}

	@Override
	public boolean processIt(String processAction)  {
		processMsg = null;
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(processAction, getDocAction());
	}

	@Override
	public boolean unlockIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean invalidateIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String prepareIt() {
		log.info(toString());
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Std Period open?
		MPeriod.testPeriodOpen(getCtx(), getMovementDate(), MDocType.DOCBASETYPE_ProjectIssue, getAD_Org_ID(), get_TrxName());
		
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}

	@Override
	public boolean approveIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rejectIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String completeIt() {

		//	Re-Check
		if (!justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		log.info(toString());
		if (getM_Product_ID() == 0)
		{
			log.log(Level.SEVERE, "No Product");
			return DocAction.STATUS_Invalid;
		}

		MProduct product = MProduct.get (getCtx(), getM_Product_ID());

		//	If not a stocked Item nothing to do
		if (!product.isStocked())
		{
			setProcessed(true);
			setDocAction(DOCACTION_Close);
			saveEx();
			return DocAction.STATUS_Completed;
		}

		/** @todo Transaction */

		//Ignore the Material Policy when is Reverse Correction
		if(!isReversal())
			checkMaterialPolicy();
		
		if (getM_AttributeSetInstance_ID() == 0) {
			String whereClause = "C_ProjectIssue_ID=?";
			List<X_C_ProjectIssueMA> mas = new Query(getCtx(), X_C_ProjectIssueMA.Table_Name, whereClause, get_TrxName())
					.setClient_ID()
					.setParameters(getC_ProjectIssue_ID())
					.list();
			for (X_C_ProjectIssueMA ma : mas)
			{
				BigDecimal QtyMA = ma.getMovementQty().negate();
				BigDecimal reservedDiff = Env.ZERO;
				BigDecimal orderedDiff = Env.ZERO;
				//	Update Storage - see also VMatch.createMatchRecord
				if (!MStorage.add(getCtx(), getM_Locator().getM_Warehouse_ID(),
						getM_Locator_ID(),
						getM_Product_ID(),
						ma.getM_AttributeSetInstance_ID(), 0,
						QtyMA,reservedDiff	,orderedDiff, get_TrxName()))
				{
					processMsg = "Cannot correct Inventory (MA)";
					return DocAction.STATUS_Invalid;
				}
				//	Create Transaction
				MTransaction materialTransaction = new MTransaction (getCtx(), getAD_Org_ID(),
						MTransaction.MOVEMENTTYPE_WorkOrder_, getM_Locator_ID(),
						getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
						QtyMA, getMovementDate(), get_TrxName());
				materialTransaction.setC_ProjectIssue_ID(getC_ProjectIssue_ID());
				if (!materialTransaction.save())
				{
					processMsg = "Could not create Material Transaction (MA)";
					return DocAction.STATUS_Invalid;
				}
				
			}		
		}
		else {
			//	**	Create Material Transactions **
			MTransaction materialTransaction = new MTransaction (getCtx(), getAD_Org_ID(), 
					MTransaction.MOVEMENTTYPE_WorkOrder_,
					getM_Locator_ID(), getM_Product_ID(), getM_AttributeSetInstance_ID(),
					getMovementQty().negate(), getMovementDate(), get_TrxName());
			materialTransaction.setC_ProjectIssue_ID(getC_ProjectIssue_ID());
			//
			MLocator loc = MLocator.get(getCtx(), getM_Locator_ID());
			if (MStorage.add(getCtx(), loc.getM_Warehouse_ID(), getM_Locator_ID(), 
					getM_Product_ID(), getM_AttributeSetInstance_ID(), 0,
					getMovementQty().negate(), Env.ZERO	, Env.ZERO, get_TrxName()))
			{
				if (!materialTransaction.save())
				{
					processMsg = "Transaction not inserted(2)";
					return DocAction.STATUS_Invalid;
				}			
			}
		}

		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		setProcessed(true);
		setDocStatus("CO");
		setDocAction(DOCACTION_Close);
		saveEx();
		return DocAction.STATUS_Completed;
	
	}

	@Override
	public boolean voidIt() {
		if (log.isLoggable(Level.INFO))
			log.info(toString());
		// Before Void
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_VOID);
		if (processMsg != null)
			return false;

		if (DOCSTATUS_Closed.equals(getDocStatus()) || DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			processMsg = "Document Closed: " + getDocStatus();
			setDocAction(DOCACTION_None);
			return false;
		}

		// Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus()) || DOCSTATUS_Invalid.equals(getDocStatus())
				|| DOCSTATUS_Approved.equals(getDocStatus()) || DOCSTATUS_NotApproved.equals(getDocStatus())
				|| DOCSTATUS_InProgress.equals(getDocStatus()))
		{
			setMovementQty(Env.ZERO);
		}
		else
		{
			boolean accrual = false;
			try
			{
				MPeriod.testPeriodOpen(getCtx(), getMovementDate(), MDocType.DOCBASETYPE_ManufacturingOrder, getAD_Org_ID());
			}
			catch (PeriodClosedException e)
			{
				accrual = true;
			}

			if (accrual)
				return reverseAccrualIt();
			else
				return reverseCorrectIt();
		}

		// After Void
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
		if (processMsg != null)
			return false;

		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	
	}

	@Override
	public boolean closeIt() {
		if (log.isLoggable(Level.INFO))
			log.info(toString());
		// Before Close
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_CLOSE);
		if (processMsg != null)
			return false;

		setProcessed(true);
		setDocAction(DOCACTION_None);

		// After Close
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_CLOSE);
		if (processMsg != null)
			return false;
		
		return true;
	}

	@Override
	public boolean reverseCorrectIt() {
		if (log.isLoggable(Level.INFO))
			log.info(toString());
		// Before reverseCorrect
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (processMsg != null)
			return false;

		MProjectIssue reversal = reverseIt(false);
		if (reversal == null)
			return false;
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (processMsg != null)
			return false;

		processMsg = reversal.getDocumentNo();

		return true;
	}

	@Override
	public boolean reverseAccrualIt() {
		if (log.isLoggable(Level.INFO))
			log.info(toString());
		// Before reverseAccrual
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (processMsg != null)
			return false;

		MProjectIssue reversal = reverseIt(true);
		if (reversal == null)
			return false;

		// After reverseAccrual
		processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (processMsg != null)
			return false;

		processMsg = reversal.getDocumentNo();

		return true;
	
	}

	@Override
	public boolean reActivateIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSummary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDocumentInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File createPDF() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDoc_User_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BigDecimal getApprovalAmt() {
		// TODO Auto-generated method stub
		return null;
	}
	

	/**
	 * Check Material Plicity
	 * @param pLine
	 * @param MovementType
	 */
	public void checkMaterialPolicy()
	{
		int no = deleteMA();
		if (no > 0)
			log.config("Delete old #" + no);
		//	Incoming Trx
		boolean needSave = false;
		MProduct product = (MProduct)getM_Product();

		String MMPolicy = product.getMMPolicy();
		Timestamp minGuaranteeDate = getMovementDate();
		MStorage[] storages = MStorage.getWarehouse(getCtx(), getM_Locator().getM_Warehouse_ID(), getM_Product_ID(), getM_AttributeSetInstance_ID(),
				minGuaranteeDate, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, getM_Locator_ID(), get_TrxName());
		BigDecimal qtyToDeliver = getMovementQty();
		for (MStorage storage: storages)
		{
			if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
			{				
				X_C_ProjectIssueMA c_ProjectIssueMA = new X_C_ProjectIssueMA(getCtx(), 0, get_TrxName());
				c_ProjectIssueMA.setC_ProjectIssue_ID(getC_ProjectIssue_ID());
				c_ProjectIssueMA.setM_AttributeSetInstance_ID(storage.getM_AttributeSetInstance_ID());
				c_ProjectIssueMA.setMovementQty(qtyToDeliver);
				c_ProjectIssueMA.saveEx();
				qtyToDeliver = Env.ZERO;
			}
			else
			{

				X_C_ProjectIssueMA c_ProjectIssueMA = new X_C_ProjectIssueMA(getCtx(), 0, get_TrxName());
				c_ProjectIssueMA.setC_ProjectIssue_ID(getC_ProjectIssue_ID());
				c_ProjectIssueMA.setM_AttributeSetInstance_ID(storage.getM_AttributeSetInstance_ID());
				c_ProjectIssueMA.setMovementQty(storage.getQtyOnHand());
				c_ProjectIssueMA.saveEx();
				qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
				log.fine( c_ProjectIssueMA + ", QtyToDeliver=" + qtyToDeliver);
			}

			if (qtyToDeliver.signum() == 0)
				break;
		}


		if (needSave)
		{
			saveEx();
		}
	}	//	checkMaterialPolicy
	


	public int deleteMA() {
		String sql = "DELETE FROM C_ProjectIssueMA WHERE C_ProjectIssue_ID = " + get_ID();
		int count = DB.executeUpdateEx( sql, get_TrxName() );
		return count;
	}
	

	/** Reversal Flag		*/
	private boolean m_reversal = false;
	
	/**
	 * 	Set Reversal
	 *	@param reversal reversal
	 */
	public void setReversal(boolean reversal)
	{
		m_reversal = reversal;
	}	//	setReversal
	/**
	 * 	Is Reversal
	 *	@return reversal
	 */
	public boolean isReversal()
	{
		return m_reversal;
	}	//	isReversal
	
	public MProjectIssue reverseIt(boolean isAccrual)
	{
		Timestamp currentDate = new Timestamp(System.currentTimeMillis());
		Optional<Timestamp> loginDateOptional = Optional.of(Env.getContextAsDate(getCtx(),"#Date"));
		Timestamp reversalDate =  isAccrual ? loginDateOptional.orElse(currentDate) : getMovementDate();
		MPeriod.testPeriodOpen(getCtx(), reversalDate , getC_DocType_ID(), getAD_Org_ID());
		MProjectIssue reversal = null;
		reversal = copyFrom(reversalDate);
		MDocType docType = MDocType.get(getCtx(), getC_DocType_ID());
		if(docType.isCopyDocNoOnReversal()) {
			reversal.setDocumentNo(getDocumentNo() + Msg.getMsg(getCtx(), "^"));
		}

		StringBuilder msgadd = new StringBuilder("{->").append(getDocumentNo()).append(")");
		reversal.addDescription(msgadd.toString());
		reversal.setReversal_ID(getC_ProjectIssue_ID());
		reversal.saveEx(get_TrxName());
		copyProjectMA(reversal);

		if (!reversal.processIt(DocAction.ACTION_Complete))
		{
			processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
			return null;
		}

		reversal.closeIt();
		reversal.setProcessing(false);
		reversal.setDocStatus(DOCSTATUS_Reversed);
		reversal.setDocAction(DOCACTION_None);
		reversal.saveEx(get_TrxName());

		msgadd = new StringBuilder("(").append(reversal.getDocumentNo()).append("<-)");
		addDescription(msgadd.toString());

		setProcessed(true);
		setReversal_ID(reversal.getC_ProjectIssue_ID());
		setDocStatus(DOCSTATUS_Reversed); // may come from void
		setDocAction(DOCACTION_None);
		return reversal;
	}
	
	private MProjectIssue copyFrom(Timestamp reversalDate)
	{
		MProjectIssue to = new MProjectIssue(getCtx(), 0, get_TrxName());
		PO.copyValues(this, to, getAD_Client_ID(), getAD_Org_ID());
		to.set_ValueNoCheck("DocumentNo", null);
		//
		to.setDocStatus(DOCSTATUS_Drafted); // Draft
		to.setDocAction(DOCACTION_Complete);
		to.setMovementDate(reversalDate);	
		to.setReversal(true);	
		//to.set_Value("IsComplete", "N");
		//to.set_Value("IsCreated", "Y");
		to.setPosted(false);
		to.setProcessing(false);
		to.setProcessed(false);
		to.setMovementQty(getMovementQty().negate());
		to.setReversal_ID(getC_ProjectIssue_ID());
		to.saveEx();
		return to;
	}
	

	public void addDescription(String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
		{
			StringBuilder msgd = new StringBuilder(desc).append(" | ").append(description);
			setDescription(msgd.toString());
		}
	} // addDescription
	
	

	private void copyProjectMA(MProjectIssue reversal) {
		String whereClause = "C_ProjectIssue_ID=?";
		List<X_C_ProjectIssueMA> mas = new Query(getCtx(), X_C_ProjectIssueMA.Table_Name, whereClause, get_TrxName())
				.setClient_ID()
				.setParameters(reversal.getReversal_ID())
				.list();
		for (X_C_ProjectIssueMA projectIssueMA_Orginal : mas)
		{
			X_C_ProjectIssueMA projectIssueMA_New = new X_C_ProjectIssueMA(getCtx(), 0, get_TrxName());
			projectIssueMA_New.setC_ProjectIssue_ID(reversal.getC_ProjectIssue_ID());
			projectIssueMA_New.setM_AttributeSetInstance_ID(projectIssueMA_Orginal.getM_AttributeSetInstance_ID());
			projectIssueMA_New.setMovementQty(projectIssueMA_Orginal.getMovementQty().negate());
			projectIssueMA_New.saveEx();			
		}

	}




}	//	MProjectIssue
